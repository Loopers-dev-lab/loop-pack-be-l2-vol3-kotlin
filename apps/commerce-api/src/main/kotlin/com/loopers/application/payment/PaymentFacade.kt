package com.loopers.application.payment

import com.loopers.application.coupon.CouponService
import com.loopers.application.order.OrderService
import com.loopers.application.product.ProductService
import com.loopers.domain.order.Order
import com.loopers.domain.payment.PaymentStatus
import com.loopers.infrastructure.pg.PgApiResponse
import com.loopers.infrastructure.pg.PgCallbackRequest
import com.loopers.infrastructure.pg.PgPaymentRequest
import com.loopers.infrastructure.pg.PgPaymentResponse
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
        val paymentInfo = createPaymentInTransaction(userId, criteria)
        val pgResult = callPgSafely(userId, paymentInfo, criteria)
        return applyPgResultInTransaction(paymentInfo, pgResult)
    }

    private fun createPaymentInTransaction(userId: Long, criteria: PaymentCriteria): PaymentInfo {
        return transactionTemplate.execute {
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
        } ?: throw CoreException(ErrorType.INTERNAL_ERROR, "결제 생성 중 오류가 발생했습니다.")
    }

    private fun callPgSafely(
        userId: Long,
        paymentInfo: PaymentInfo,
        criteria: PaymentCriteria,
    ): PgApiResponse<PgPaymentResponse>? {
        return try {
            pgPaymentClient.requestPayment(
                userId.toString(),
                PgPaymentRequest(
                    orderId = paymentInfo.orderId.toString(),
                    cardType = criteria.cardType,
                    cardNo = criteria.cardNo,
                    amount = paymentInfo.amount.toLong(),
                    callbackUrl = callbackUrl,
                ),
            )
        } catch (e: CoreException) {
            log.warn("PG 결제 요청 실패: orderId=${paymentInfo.orderId}", e)
            null
        }
    }

    private fun applyPgResultInTransaction(paymentInfo: PaymentInfo, pgResult: PgApiResponse<PgPaymentResponse>?): PaymentInfo {
        return transactionTemplate.execute {
            val payment = paymentService.getPayment(paymentInfo.id)

            if (pgResult == null || !pgResult.isSuccess() || pgResult.data == null) {
                val reason = if (pgResult == null) "PG 시스템 장애" else "PG 결제 요청 실패"
                payment.markFailed(reason)
                val order = orderService.getOrder(payment.orderId)
                order.markFailed()
                compensateOrder(order)
                return@execute PaymentInfo.from(payment)
            }

            payment.markRequested(pgResult.data.transactionKey)
            PaymentInfo.from(payment)
        } ?: throw CoreException(ErrorType.INTERNAL_ERROR, "결제 처리 중 오류가 발생했습니다.")
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
