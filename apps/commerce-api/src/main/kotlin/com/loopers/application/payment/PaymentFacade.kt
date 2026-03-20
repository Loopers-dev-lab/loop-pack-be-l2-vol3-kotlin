package com.loopers.application.payment

import com.loopers.application.coupon.CouponService
import com.loopers.application.order.OrderService
import com.loopers.application.product.ProductService
import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.payment.PaymentModel

import com.loopers.domain.payment.vo.CardNo
import com.loopers.infrastructure.payment.PgProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PaymentFacade(
    private val paymentService: PaymentService,
    private val orderService: OrderService,
    private val productService: ProductService,
    private val couponService: CouponService,
    private val pgClient: PgClient,
    private val pgProperties: PgProperties,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PaymentFacade::class.java)
    }

    @Transactional
    fun requestPayment(memberId: Long, command: PaymentCommand.RequestPayment): PaymentInfo {
        val cardNo = CardNo.of(command.cardNo)

        val order = orderService.getOrderByIdWithLock(command.orderId)
        order.validateOwner(memberId)

        if (order.status != OrderStatus.ORDERED) {
            throw CoreException(ErrorType.BAD_REQUEST, "결제 요청은 ORDERED 상태에서만 가능합니다.")
        }

        if (command.amount != order.getTotalAmount()) {
            throw CoreException(ErrorType.BAD_REQUEST, "결제 금액이 주문 총액과 일치하지 않습니다.")
        }

        if (paymentService.hasActivePayment(command.orderId)) {
            throw CoreException(ErrorType.CONFLICT, "이미 진행 중인 결제가 있습니다.")
        }

        val payment = paymentService.createPayment(
            PaymentModel(
                orderId = command.orderId,
                memberId = memberId,
                cardType = command.cardType,
                cardNo = cardNo.value,
                amount = command.amount,
            ),
        )

        orderService.updateOrderStatus(command.orderId, OrderStatus.PAYMENT_PENDING)

        return PaymentInfo.from(payment)
    }

    fun callPg(paymentId: Long, memberId: Long, orderNumber: String): PaymentInfo {
        val payment = paymentService.getPayment(paymentId)

        try {
            val pgResponse = pgClient.requestPayment(
                memberId = memberId,
                request = PgPaymentRequest(
                    orderId = orderNumber,
                    cardType = payment.cardType.name,
                    cardNo = payment.cardNo,
                    amount = payment.amount,
                    callbackUrl = getCallbackUrl(),
                ),
            )
            return updatePaymentAfterPgCall(paymentId, pgResponse.transactionKey)
        } catch (e: Exception) {
            logger.warn("PG 결제 요청 실패: paymentId={}, error={}", paymentId, e.message)
            return handlePgFailure(paymentId, e.message)
        }
    }

    @Transactional
    fun updatePaymentAfterPgCall(paymentId: Long, transactionKey: String): PaymentInfo {
        val payment = paymentService.getPayment(paymentId)
        val updated = payment.assignTransactionKey(transactionKey)
        return PaymentInfo.from(paymentService.savePayment(updated))
    }

    @Transactional
    fun handlePgFailure(paymentId: Long, errorMessage: String?): PaymentInfo {
        val payment = paymentService.getPayment(paymentId)
        val failed = payment.markFailed(errorMessage)
        val saved = paymentService.savePayment(failed)

        orderService.updateOrderStatus(payment.orderId, OrderStatus.ORDERED)

        return PaymentInfo.from(saved)
    }

    @Transactional
    fun handleCallback(transactionKey: String, status: String, reason: String?) {
        val payment = paymentService.getPaymentByTransactionKeyWithLock(transactionKey)

        if (payment.isTerminal()) {
            logger.info("이미 처리된 결제 콜백 무시: transactionKey={}, status={}", transactionKey, payment.status)
            return
        }

        when (status) {
            "SUCCESS" -> handlePaymentSuccess(payment)
            "FAILED" -> handlePaymentFailure(payment, reason)
            else -> logger.warn("알 수 없는 결제 상태: transactionKey={}, status={}", transactionKey, status)
        }
    }

    private fun handlePaymentSuccess(payment: PaymentModel) {
        val succeeded = payment.markSuccess()
        paymentService.savePayment(succeeded)
        orderService.updateOrderStatus(payment.orderId, OrderStatus.PAID)
    }

    private fun handlePaymentFailure(payment: PaymentModel, reason: String?) {
        val failed = payment.markFailed(reason)
        paymentService.savePayment(failed)

        compensate(payment.orderId)
    }

    @Transactional
    fun handlePolledRequestedPayment(paymentId: Long, transactionKey: String, status: String, reason: String?) {
        val payment = paymentService.getPaymentWithLock(paymentId)

        if (payment.isTerminal()) {
            logger.info("이미 처리된 결제 무시: paymentId={}, status={}", paymentId, payment.status)
            return
        }

        val withKey = if (payment.transactionKey == null) {
            val assigned = payment.assignTransactionKey(transactionKey)
            paymentService.savePayment(assigned)
        } else {
            payment
        }

        when (status) {
            "SUCCESS" -> handlePaymentSuccess(withKey)
            "FAILED" -> handlePaymentFailure(withKey, reason)
            else -> logger.warn("알 수 없는 결제 상태: paymentId={}, status={}", paymentId, status)
        }
    }

    private fun compensate(orderId: Long) {
        val order = orderService.getOrderById(orderId)

        order.items.sortedBy { it.productId }.forEach { item ->
            productService.restoreStock(item.productId, item.quantity)
        }

        if (order.couponId != null) {
            couponService.restoreCoupon(order.couponId)
        }

        orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED)
    }

    private fun getCallbackUrl(): String {
        return pgProperties.callbackUrl
    }
}
