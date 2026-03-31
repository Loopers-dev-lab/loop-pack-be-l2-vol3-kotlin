package com.loopers.application.payment

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class PaymentPollingScheduler(
    private val paymentService: PaymentService,
    private val paymentFacade: PaymentFacade,
    private val pgClient: PgClient,
    private val orderService: com.loopers.application.order.OrderService,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PaymentPollingScheduler::class.java)
    }

    @Scheduled(fixedDelay = 30_000)
    fun pollPendingPayments() {
        val threshold = ZonedDateTime.now().minusSeconds(60)

        val pendingPayments = paymentService.getPendingPaymentsBefore(threshold)
        pendingPayments.forEach { payment ->
            try {
                val transactionKey = payment.transactionKey ?: return@forEach
                val pgStatus = pgClient.getPaymentStatus(payment.memberId, transactionKey)
                if (pgStatus.status == "SUCCESS" || pgStatus.status == "FAILED") {
                    paymentFacade.handleCallback(transactionKey, pgStatus.status, pgStatus.reason)
                }
            } catch (e: Exception) {
                logger.warn("PENDING 결제 폴링 실패: paymentId={}, error={}", payment.id, e.message)
            }
        }

        val requestedPayments = paymentService.getRequestedPaymentsBefore(threshold)
        requestedPayments.forEach { payment ->
            try {
                val order = orderService.getOrderById(payment.orderId)
                val pgResult = pgClient.getPaymentsByOrderId(payment.memberId, order.orderNumber)
                val matched = pgResult.transactions.firstOrNull()
                if (matched != null) {
                    if (matched.status == "SUCCESS" || matched.status == "FAILED") {
                        paymentFacade.handlePolledRequestedPayment(
                            payment.id,
                            matched.transactionKey,
                            matched.status,
                            matched.reason,
                        )
                    }
                } else {
                    paymentFacade.handlePgFailure(payment.id, "PG에서 결제 건을 찾을 수 없습니다.")
                }
            } catch (e: Exception) {
                logger.warn("REQUESTED 결제 폴링 실패: paymentId={}, error={}", payment.id, e.message)
            }
        }
    }
}
