package com.loopers.application.payment

import com.loopers.domain.order.OrderRepository
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.payment.PaymentStatus
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class RecoverPaymentUseCase(
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
    private val paymentGatewayPort: PaymentGatewayPort,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun recoverPendingPayments() {
        val cutoff = ZonedDateTime.now().minusMinutes(5)
        val payments = paymentRepository.findAllByStatusAndCreatedBefore(PaymentStatus.REQUESTED, cutoff)

        log.info("복구 대상 결제 {}건 조회", payments.size)

        payments.forEach { payment ->
            try {
                recoverSinglePayment(requireNotNull(payment.persistenceId))
            } catch (e: Exception) {
                log.error("결제 복구 실패. paymentId={}", payment.persistenceId, e)
            }
        }
    }

    @Transactional
    fun recoverSinglePayment(paymentId: Long): PaymentInfo {
        val payment = paymentRepository.findById(paymentId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "결제를 찾을 수 없습니다: $paymentId")

        if (payment.isTerminal()) {
            return PaymentInfo.from(payment)
        }

        val transactionKey = payment.transactionKey
            ?: throw CoreException(ErrorType.BAD_REQUEST, "transactionKey가 없는 결제는 복구할 수 없습니다: $paymentId")

        val detail = paymentGatewayPort.getTransactionStatus(payment.refUserId.toString(), transactionKey)

        val updated = when (detail.status) {
            "SUCCESS" -> {
                val approved = payment.approve()
                paymentRepository.save(approved)

                val order = orderRepository.findByIdForUpdate(payment.refOrderId)
                if (order != null) {
                    val completed = order.complete()
                    orderRepository.save(completed)
                }
                approved
            }
            "FAILED" -> {
                val failed = payment.fail(detail.reason ?: "PG 결제 실패 (복구)")
                paymentRepository.save(failed)
                failed
            }
            else -> {
                log.info("PG에서 아직 처리 중. paymentId={}, pgStatus={}", paymentId, detail.status)
                payment
            }
        }

        return PaymentInfo.from(updated)
    }
}
