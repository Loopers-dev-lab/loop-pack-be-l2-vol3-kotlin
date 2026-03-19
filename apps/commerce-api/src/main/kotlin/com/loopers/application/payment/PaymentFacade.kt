package com.loopers.application.payment

import com.loopers.application.coupon.CouponService
import com.loopers.application.order.OrderService
import com.loopers.application.product.ProductService
import com.loopers.domain.order.Order
import com.loopers.domain.payment.PaymentStatus
import com.loopers.infrastructure.pg.PgCallbackRequest
import com.loopers.infrastructure.pg.PgPaymentRequest
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

@Component
class PaymentFacade(
    private val paymentService: PaymentService,
    private val orderService: OrderService,
    private val productService: ProductService,
    private val couponService: CouponService,
    private val pgPaymentClient: PgPaymentClient,
    transactionManager: PlatformTransactionManager,
    @Value("\${pg.callback-url}") private val callbackUrl: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val transactionTemplate = TransactionTemplate(transactionManager)

    fun requestPayment(userId: Long, criteria: PaymentCriteria): PaymentInfo {
        val paymentInfo = transactionTemplate.execute {
            val order = orderService.getOrder(criteria.orderId)
            order.validateOwner(userId)
            order.validatePayable()

            paymentService.getPaymentByOrderId(criteria.orderId)?.let {
                throw CoreException(ErrorType.CONFLICT, "이미 결제가 진행 중인 주문입니다.")
            }

            val payment = paymentService.createPayment(
                userId,
                order.id,
                order.totalAmount,
                criteria.cardType,
                criteria.cardNo,
            )
            PaymentInfo.from(payment)
        }!!

        return transactionTemplate.execute {
            val payment = paymentService.getPayment(paymentInfo.id)

            val pgRequest = PgPaymentRequest(
                orderId = payment.orderId.toString(),
                cardType = criteria.cardType,
                cardNo = criteria.cardNo,
                amount = payment.amount.toLong(),
                callbackUrl = callbackUrl,
            )

            try {
                val pgResponse = pgPaymentClient.requestPayment(userId.toString(), pgRequest)

                if (!pgResponse.isSuccess() || pgResponse.data == null) {
                    payment.markFailed("PG 결제 요청 실패")
                    val order = orderService.getOrder(payment.orderId)
                    order.markFailed()
                    compensateOrder(order)
                    return@execute PaymentInfo.from(payment)
                }

                payment.markRequested(pgResponse.data.transactionKey)
            } catch (e: CoreException) {
                payment.markFailed(e.message)
                val order = orderService.getOrder(payment.orderId)
                order.markFailed()
                compensateOrder(order)
                throw e
            }

            PaymentInfo.from(payment)
        }!!
    }

    @Transactional
    fun handleCallback(callbackRequest: PgCallbackRequest) {
        val payment = paymentService.getPaymentByTransactionKey(callbackRequest.transactionKey)
            ?: throw CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다.")

        if (payment.status != PaymentStatus.REQUESTED) return

        val isSuccess = callbackRequest.status == "SUCCESS"
        val order = orderService.getOrder(payment.orderId)

        if (isSuccess) {
            payment.markPaid()
            order.markPaid()
        } else {
            payment.markFailed(callbackRequest.reason)
            order.markFailed()
            compensateOrder(order)
        }
    }

    @Transactional
    fun getPaymentStatus(userId: Long, orderId: Long): PaymentInfo {
        val order = orderService.getOrder(orderId)
        order.validateOwner(userId)

        val payment = paymentService.getPaymentByOrderId(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다.")

        if (payment.status == PaymentStatus.REQUESTED && payment.transactionKey != null) {
            val pgResponse = pgPaymentClient.getPaymentStatus(userId.toString(), payment.transactionKey!!)
            val pgDetail = pgResponse.data
            if (pgDetail != null && pgDetail.isTerminal()) {
                if (pgDetail.isSuccess()) {
                    payment.markPaid()
                    order.markPaid()
                } else {
                    payment.markFailed(pgDetail.reason)
                    order.markFailed()
                    compensateOrder(order)
                }
            }
        }

        return PaymentInfo.from(payment)
    }

    private fun compensateOrder(order: Order) {
        try {
            val stockItems = order.orderItems.map { it.productId to it.quantity }
            productService.restoreStock(stockItems)

            order.couponId?.let { couponId ->
                couponService.restoreCoupon(couponId)
            }
        } catch (e: Exception) {
            log.error("주문 보상 처리 실패: orderId=${order.id}", e)
        }
    }
}
