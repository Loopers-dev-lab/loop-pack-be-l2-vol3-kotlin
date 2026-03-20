package com.loopers.infrastructure.payment

import com.loopers.domain.payment.CompletePaymentCommand
import com.loopers.domain.payment.FailPaymentCommand
import com.loopers.domain.payment.PaymentInfo
import com.loopers.domain.payment.PaymentService
import com.loopers.domain.payment.PgClient
import com.loopers.support.error.CoreException
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
@Profile("!test")
class PendingPaymentScheduler(
    private val paymentService: PaymentService,
    private val pgClient: PgClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val PENDING_TIMEOUT_MINUTES = 10L
    }

    @Scheduled(fixedDelay = 60_000)
    fun syncPendingPayments() {
        val cutoff = ZonedDateTime.now().minusMinutes(PENDING_TIMEOUT_MINUTES)
        val pendingPayments = paymentService.getPendingPayments(cutoff)

        if (pendingPayments.isEmpty()) return

        log.info("PENDING 결제 동기화 시작: {}건", pendingPayments.size)

        pendingPayments.forEach { payment ->
            try {
                processPayment(payment)
            } catch (e: Exception) {
                log.warn("PENDING 결제 처리 실패: paymentId={}, error={}", payment.id, e.message)
            }
        }
    }

    private fun processPayment(payment: PaymentInfo) {
        val transactionKey = payment.transactionKey
        if (transactionKey == null) {
            paymentService.failPaymentById(payment.id, "PG 트랜잭션 키 미할당 상태에서 타임아웃")
            log.info("스케줄러 transactionKey 없는 결제 실패 처리: paymentId={}", payment.id)
            return
        }

        try {
            val pgStatus = pgClient.getPaymentStatus(payment.userId, transactionKey)
            when (pgStatus.status.uppercase()) {
                "SUCCESS" -> {
                    paymentService.completePayment(CompletePaymentCommand(transactionKey = transactionKey))
                    log.info("스케줄러 결제 성공 처리: paymentId={}", payment.id)
                }
                "FAILED" -> {
                    paymentService.failPayment(
                        FailPaymentCommand(transactionKey = transactionKey, reason = pgStatus.reason),
                    )
                    log.info("스케줄러 결제 실패 처리: paymentId={}", payment.id)
                }
                else -> {
                    log.info("스케줄러 PG 상태 미확정: paymentId={}, pgStatus={}", payment.id, pgStatus.status)
                }
            }
        } catch (e: CoreException) {
            log.warn("스케줄러 PG 조회 실패: paymentId={}, error={}", payment.id, e.message)
        }
    }
}
