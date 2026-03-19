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
        val payments = paymentRepository.findAllByStatusInAndCreatedBefore(
            listOf(PaymentStatus.PENDING, PaymentStatus.REQUESTED),
            cutoff,
        )

        log.info("복구 대상 결제 {}건 조회", payments.size)

        payments.forEach { payment ->
            try {
                recoverSinglePayment(requireNotNull(payment.persistenceId))
            } catch (e: Exception) {
                log.error("결제 복구 실패. paymentId={}", payment.persistenceId, e)
            }
        }
    }

    fun recoverSinglePayment(paymentId: Long): PaymentInfo {
        val payment = paymentRepository.findById(paymentId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "결제를 찾을 수 없습니다: $paymentId")

        if (payment.isTerminal()) {
            return PaymentInfo.from(payment)
        }

        val transactionKey = payment.transactionKey
        if (transactionKey != null) {
            val detail = paymentGatewayPort.getTransactionStatus(payment.refUserId.toString(), transactionKey)
            return applyRecoveryResult(paymentId, detail)
        }

        return recoverByOrderId(paymentId, payment.refUserId, payment.refOrderId)
    }

    private fun recoverByOrderId(paymentId: Long, userId: Long, orderId: Long): PaymentInfo {
        val paddedOrderId = orderId.toString().padStart(6, '0')
        val transactions = try {
            paymentGatewayPort.getTransactionsByOrderId(userId.toString(), paddedOrderId)
        } catch (e: Exception) {
            log.warn("PG orderId 조회 실패. paymentId={}, orderId={}", paymentId, orderId, e)
            return markFailedAndReturn(paymentId, "PG 조회 실패로 결제를 종료합니다.")
        }

        if (transactions.isEmpty()) {
            return markFailedAndReturn(paymentId, "PG에 해당 거래가 존재하지 않습니다.")
        }

        val latest = transactions.first()
        return applyRecoveryResult(paymentId, latest)
    }

    private fun markFailedAndReturn(paymentId: Long, reason: String): PaymentInfo {
        val payment = paymentRepository.findByIdForUpdate(paymentId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "결제를 찾을 수 없습니다: $paymentId")

        if (payment.isTerminal()) {
            return PaymentInfo.from(payment)
        }

        val failed = payment.fail(reason)
        paymentRepository.save(failed)
        return PaymentInfo.from(failed)
    }

    @Transactional
    fun applyRecoveryResult(paymentId: Long, detail: PgTransactionDetail): PaymentInfo {
        val payment = paymentRepository.findByIdForUpdate(paymentId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "결제를 찾을 수 없습니다: $paymentId")

        if (payment.isTerminal()) {
            return PaymentInfo.from(payment)
        }

        val withTxKey = if (payment.transactionKey == null && detail.transactionKey.isNotBlank()) {
            val requested = payment.markRequested(detail.transactionKey)
            paymentRepository.save(requested)
            requested
        } else {
            payment
        }

        val updated = when (detail.status) {
            "SUCCESS" -> {
                val toApprove = if (withTxKey.status == PaymentStatus.REQUESTED) {
                    withTxKey
                } else {
                    val requested = withTxKey.markRequested(detail.transactionKey)
                    paymentRepository.save(requested)
                    requested
                }
                val approved = toApprove.approve()
                paymentRepository.save(approved)

                val order = orderRepository.findByIdForUpdate(payment.refOrderId)
                if (order != null) {
                    val completed = order.complete()
                    orderRepository.save(completed)
                }
                approved
            }
            "FAILED" -> {
                val failed = withTxKey.fail(detail.reason ?: "PG 결제 실패 (복구)")
                paymentRepository.save(failed)
                failed
            }
            else -> {
                log.info("PG에서 아직 처리 중. paymentId={}, pgStatus={}", paymentId, detail.status)
                withTxKey
            }
        }

        return PaymentInfo.from(updated)
    }
}
