package com.loopers.domain.payment

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PaymentService(
    private val paymentRepository: PaymentRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun createPayment(command: CreatePaymentCommand): Payment {
        val payment = Payment(
            orderId = command.orderId,
            userId = command.userId,
            amount = command.amount,
            cardType = command.cardType,
            cardNo = command.cardNo,
        )
        return paymentRepository.save(payment)
    }

    /**
     * PG 상태를 기반으로 Payment 상태를 동기화.
     * Order 상태 변경은 Facade에서 처리 (DIP 준수).
     *
     * @return 동기화된 PG 상태 ("SUCCESS", "FAILED", "PENDING")
     */
    @Transactional
    fun syncPaymentStatus(paymentId: Long, pgStatus: String, pgReason: String?): String {
        when (pgStatus) {
            "SUCCESS" -> {
                val payment = findById(paymentId)
                if (payment.paymentStatus != PaymentStatus.APPROVED) {
                    val transactionKey = payment.transactionKey
                        ?: throw CoreException(ErrorType.BAD_REQUEST, "transactionKey가 없는 결제건은 승인 처리할 수 없습니다.")
                    markApproved(paymentId, transactionKey)
                }
            }
            "FAILED" -> {
                val payment = findById(paymentId)
                if (payment.paymentStatus != PaymentStatus.REJECTED) {
                    markRejected(paymentId, pgReason ?: "알 수 없는 사유")
                }
            }
            "PENDING" -> {
                log.info("PG 아직 처리 중 - paymentId: {}", paymentId)
            }
        }
        return pgStatus
    }

    @Transactional
    fun updateTransactionKey(paymentId: Long, transactionKey: String) {
        val payment = findById(paymentId)
        payment.updateTransactionKey(transactionKey)
    }

    @Transactional
    fun markApproved(paymentId: Long, transactionKey: String) {
        val payment = findById(paymentId)
        payment.markApproved(transactionKey)
    }

    @Transactional
    fun markRejected(paymentId: Long, failReason: String) {
        val payment = findById(paymentId)
        payment.markRejected(failReason)
    }

    @Transactional
    fun markTimeout(paymentId: Long) {
        val payment = findById(paymentId)
        payment.markTimeout()
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): Payment {
        return paymentRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 결제입니다.")
    }

    @Transactional(readOnly = true)
    fun findByTransactionKey(transactionKey: String): Payment {
        return paymentRepository.findByTransactionKey(transactionKey)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 결제 트랜잭션입니다.")
    }

    @Transactional(readOnly = true)
    fun findByOrderId(orderId: Long): List<Payment> {
        return paymentRepository.findByOrderId(orderId)
    }

    @Transactional(readOnly = true)
    fun findPendingPayments(): List<Payment> {
        return paymentRepository.findByPaymentStatusIn(
            listOf(PaymentStatus.REQUESTED, PaymentStatus.TIMEOUT),
        )
    }
}
