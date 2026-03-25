package com.loopers.application.payment

import com.loopers.application.payment.event.PaymentConfirmedEvent
import com.loopers.application.payment.event.PaymentFailedEvent
import com.loopers.domain.payment.PaymentService
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Service
class PaymentCallbackFacade(
    private val paymentService: PaymentService,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val PG_STATUS_SUCCESS = "SUCCESS"
    }

    @Transactional
    fun handleCallback(cmd: PaymentCallbackCommand): PaymentResult {
        val payment = paymentService.getByTransactionKey(cmd.transactionKey)

        // 멱등성: 이미 최종 상태이면 무시
        if (payment.isTerminal()) {
            log.info("[Callback] 이미 최종 상태: transactionKey=${cmd.transactionKey}, status=${payment.status}")
            return PaymentResult.from(payment)
        }

        val now = ZonedDateTime.now()

        if (cmd.status == PG_STATUS_SUCCESS) {
            payment.confirmPaid(now)
            paymentService.updatePaymentStatus(payment)

            // 이벤트 발행 (AFTER_COMMIT: 주문 상태 변경, 행동 로깅)
            eventPublisher.publishEvent(
                PaymentConfirmedEvent(
                    paymentId = payment.id,
                    orderId = payment.orderId,
                    transactionKey = cmd.transactionKey,
                    amount = payment.amount,
                    paidAt = now,
                ),
            )
            log.info("[Callback] 결제 성공: orderId=${payment.orderId}, transactionKey=${cmd.transactionKey}")
        } else {
            payment.confirmFailed(cmd.reason, now)
            paymentService.updatePaymentStatus(payment)

            eventPublisher.publishEvent(
                PaymentFailedEvent(
                    paymentId = payment.id,
                    orderId = payment.orderId,
                    reason = cmd.reason,
                ),
            )
            log.info("[Callback] 결제 실패: orderId=${payment.orderId}, reason=${cmd.reason}")
        }

        return PaymentResult.from(payment)
    }
}
