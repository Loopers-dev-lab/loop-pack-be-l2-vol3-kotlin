package com.loopers.application.user.payment

import com.loopers.domain.order.OrderRepository
import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentReasonCode
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.payment.PgPaymentOrderResponse
import com.loopers.domain.payment.PgPaymentPort
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class PaymentReconcileUseCase(
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
    private val pgPaymentPort: PgPaymentPort,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun reconcile(paymentId: Long, userId: Long): PaymentResult.Reconciled {
        val payment = paymentRepository.findById(paymentId)
            ?: throw CoreException(ErrorType.PAYMENT_NOT_FOUND)

        if (payment.userId != userId) {
            throw CoreException(ErrorType.PAYMENT_NOT_FOUND)
        }

        val (reconciledPayment, reconcileStatus) = if (payment.isManualReconcileTarget()) {
            reconcileTimeoutPayment(payment)
        } else {
            payment to PaymentResult.ReconcileStatus.NOT_APPLICABLE
        }

        val order = orderRepository.findById(reconciledPayment.orderId)
            ?: throw CoreException(ErrorType.ORDER_NOT_FOUND)

        return PaymentResult.Reconciled.from(reconciledPayment, order.status, reconcileStatus)
    }

    private fun reconcileTimeoutPayment(
        payment: Payment,
    ): Pair<Payment, PaymentResult.ReconcileStatus> {
        val orderResponse = try {
            pgPaymentPort.queryPaymentsByOrderId(payment.orderId, payment.userId)
        } catch (e: Exception) {
            log.warn("Manual reconcile: PG 주문 조회 실패. paymentId={}, error={}", payment.id, e.message)
            return payment to PaymentResult.ReconcileStatus.QUERY_FAILED
        }

        val candidates = orderResponse.transactions.filter { it.matches(payment) }
        if (candidates.isEmpty()) {
            return payment to PaymentResult.ReconcileStatus.NOT_FOUND_IN_PG
        }
        if (candidates.size > 1) {
            return payment to PaymentResult.ReconcileStatus.AMBIGUOUS
        }

        val candidate = candidates.single()
        return when (candidate.status.uppercase()) {
            "SUCCESS" -> {
                val succeeded = payment.succeed(candidate.transactionKey)
                if (!paymentRepository.saveIfPending(succeeded)) {
                    latestPayment(payment.id!!) to PaymentResult.ReconcileStatus.ALREADY_RECONCILED
                } else {
                    val order = orderRepository.findById(payment.orderId)
                        ?: throw CoreException(ErrorType.ORDER_NOT_FOUND)
                    orderRepository.save(order.confirm())
                    succeeded to PaymentResult.ReconcileStatus.RESOLVED_SUCCESS
                }
            }
            "FAILED" -> {
                val failed = payment.attachTransactionKey(candidate.transactionKey)
                    .fail(PaymentReasonCode.fromPgReason(candidate.reason))
                if (!paymentRepository.saveIfPending(failed)) {
                    latestPayment(payment.id!!) to PaymentResult.ReconcileStatus.ALREADY_RECONCILED
                } else {
                    failed to PaymentResult.ReconcileStatus.RESOLVED_FAILURE
                }
            }
            else -> {
                val tracked = payment.attachTransactionKey(candidate.transactionKey)
                if (!paymentRepository.saveIfPending(tracked)) {
                    latestPayment(payment.id!!) to PaymentResult.ReconcileStatus.ALREADY_RECONCILED
                } else {
                    tracked to PaymentResult.ReconcileStatus.STILL_PENDING
                }
            }
        }
    }

    private fun latestPayment(paymentId: Long): Payment =
        paymentRepository.findById(paymentId)
            ?: throw CoreException(ErrorType.PAYMENT_NOT_FOUND)

    private fun Payment.isManualReconcileTarget(): Boolean =
        status == Payment.Status.PENDING &&
            transactionKey == null &&
            reasonCode == PaymentReasonCode.TIMEOUT_UNCERTAIN

    private fun PgPaymentOrderResponse.Transaction.matches(payment: Payment): Boolean =
        orderId == payment.orderId.toString() &&
            cardType.equals(payment.cardType, ignoreCase = true) &&
            sameAmount(payment.amount.amount) &&
            cardNo.cardLast4() == payment.maskedCardNo.cardLast4()

    private fun PgPaymentOrderResponse.Transaction.sameAmount(expected: BigDecimal): Boolean =
        BigDecimal.valueOf(amount).compareTo(expected) == 0

    private fun String.cardLast4(): String =
        filter(Char::isDigit).takeLast(4)
}
