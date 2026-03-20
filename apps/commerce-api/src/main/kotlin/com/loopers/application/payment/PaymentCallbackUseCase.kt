package com.loopers.application.payment

import com.loopers.application.UseCase
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.order.OrderService
import com.loopers.domain.payment.CompletePaymentCommand
import com.loopers.domain.payment.FailPaymentCommand
import com.loopers.domain.payment.PaymentService
import com.loopers.domain.payment.PaymentStatus
import com.loopers.domain.order.CancelOrderCommand
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PaymentCallbackUseCase(
    private val paymentService: PaymentService,
    private val orderService: OrderService,
    private val couponService: CouponService,
) : UseCase<PaymentCallbackCriteria, Unit> {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(criteria: PaymentCallbackCriteria) {
        val payment = paymentService.getPaymentByTransactionKey(criteria.transactionKey)

        if (payment.status != PaymentStatus.PENDING) {
            log.info("이미 처리된 결제 건: transactionKey={}, status={}", criteria.transactionKey, payment.status)
            return
        }

        when (criteria.status.uppercase()) {
            "SUCCESS" -> {
                paymentService.completePayment(CompletePaymentCommand(transactionKey = criteria.transactionKey))
                log.info("결제 성공 처리: transactionKey={}", criteria.transactionKey)
            }
            "FAILED" -> {
                paymentService.failPayment(
                    FailPaymentCommand(transactionKey = criteria.transactionKey, reason = criteria.reason),
                )
                restoreOrderResources(payment.orderId, payment.userId)
                log.info("결제 실패 처리: transactionKey={}, reason={}", criteria.transactionKey, criteria.reason)
            }
            else -> {
                log.warn("알 수 없는 콜백 상태: status={}", criteria.status)
            }
        }
    }

    private fun restoreOrderResources(orderId: Long, userId: Long) {
        try {
            val order = orderService.getOrder(orderId, userId)
            orderService.cancelOrder(CancelOrderCommand(orderId = orderId, userId = userId))
            order.issuedCouponId?.let { couponService.restoreUsedCoupon(it) }
        } catch (e: Exception) {
            log.error("주문 리소스 복구 실패: orderId={}, error={}", orderId, e.message)
        }
    }
}
