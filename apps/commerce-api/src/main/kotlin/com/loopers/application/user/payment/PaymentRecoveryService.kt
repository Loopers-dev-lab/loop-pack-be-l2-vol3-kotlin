package com.loopers.application.user.payment

import com.loopers.domain.order.OrderRepository
import com.loopers.domain.payment.PaymentReasonCode
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.payment.PgPaymentPort
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

@Service
class PaymentRecoveryService(
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
    private val pgPaymentPort: PgPaymentPort,
    private val recoveryScheduler: ScheduledExecutorService,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun scheduleEagerRetry(paymentId: Long) {
        val delays = longArrayOf(5, 30, 120)
        for (delay in delays) {
            recoveryScheduler.schedule(
                { queryAndReconcile(paymentId) },
                delay,
                TimeUnit.SECONDS,
            )
        }
        log.info("Eager retry 스케줄링 완료: paymentId={}, delays=5s/30s/2m", paymentId)
    }

    @Transactional
    fun queryAndReconcile(paymentId: Long) {
        val payment = paymentRepository.findById(paymentId)
        if (payment == null) {
            log.warn("Recovery: 존재하지 않는 paymentId={}", paymentId)
            return
        }

        if (payment.isTerminal) {
            log.debug("Recovery: 이미 terminal 상태. paymentId={}, status={}", paymentId, payment.status)
            return
        }

        if (payment.transactionKey == null) {
            log.debug("Recovery: transactionKey=null, PG 조회 skip. paymentId={}", paymentId)
            return
        }

        try {
            val pgStatus = pgPaymentPort.queryPaymentStatus(payment.transactionKey, payment.userId)

            when (pgStatus.status.uppercase()) {
                "SUCCESS" -> {
                    val succeeded = payment.succeed(pgStatus.transactionKey)
                    paymentRepository.save(succeeded)

                    val order = orderRepository.findById(payment.orderId)!!
                    val confirmed = order.confirm()
                    orderRepository.save(confirmed)

                    log.info("Recovery 완료: paymentId={}, PG status=SUCCESS", paymentId)
                }
                "FAILED" -> {
                    val reasonCode = PaymentReasonCode.fromPgReason(pgStatus.reason)
                    val failed = payment.fail(reasonCode)
                    paymentRepository.save(failed)

                    log.info("Recovery 완료: paymentId={}, PG status=FAILED", paymentId)
                }
                else -> {
                    log.debug("Recovery: PG 아직 PENDING. paymentId={}", paymentId)
                }
            }
        } catch (e: Exception) {
            log.warn("Recovery: PG 상태 조회 실패. paymentId={}, error={}", paymentId, e.message)
        }
    }

    @Scheduled(fixedRate = 300_000)
    fun sweepAgedPending() {
        val threshold = ZonedDateTime.now().minusMinutes(5)
        val pendingPayments = paymentRepository.findPendingOlderThan(threshold)

        if (pendingPayments.isEmpty()) return

        log.info("Aged pending sweep 시작: {}건", pendingPayments.size)

        for (payment in pendingPayments) {
            try {
                queryAndReconcile(payment.id!!)
            } catch (e: Exception) {
                log.warn("Aged pending sweep 실패: paymentId={}, error={}", payment.id, e.message)
            }
        }
    }
}
