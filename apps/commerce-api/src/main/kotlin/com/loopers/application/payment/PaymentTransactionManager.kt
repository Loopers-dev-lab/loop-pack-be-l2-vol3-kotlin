package com.loopers.application.payment

import com.loopers.domain.order.OrderRepository
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.product.Money
import com.loopers.domain.payment.Payment
import org.slf4j.LoggerFactory
import com.loopers.domain.payment.PaymentHistory
import com.loopers.domain.payment.PaymentHistoryRepository
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.payment.PaymentStatus
import com.loopers.support.error.PaymentException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PaymentTransactionManager(
    private val paymentRepository: PaymentRepository,
    private val paymentHistoryRepository: PaymentHistoryRepository,
    private val orderRepository: OrderRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    @Transactional
    fun saveOrResetPayment(command: PaymentCommand.Request): Payment {
        val order = orderRepository.findByIdOrNull(command.orderId)
            ?: throw PaymentException.orderNotFound()

        val existing = paymentRepository.findByOrderId(order.id)

        if (existing != null) {
            return when (existing.status) {
                PaymentStatus.APPROVED -> throw PaymentException.alreadyPaid()
                PaymentStatus.REQUESTED -> throw PaymentException.alreadyInProgress()
                PaymentStatus.FAILED,
                PaymentStatus.REQUEST_FAILED -> {
                    paymentHistoryRepository.save(PaymentHistory.from(existing))
                    existing.resetForRetry()
                    if (order.status == OrderStatus.CANCELLED) {
                        order.reorder()
                        log.info("결제 재시도로 주문 복원 [orderId={}]", order.id)
                    }
                    existing
                }
            }
        }

        val payment = Payment.create(
            orderId = order.id,
            userId = command.userId,
            amount = Money(order.totalAmount.amount),
            cardType = command.cardType,
            cardNo = command.cardNo,
        )
        return paymentRepository.save(payment)
    }

    @Transactional
    fun updateTransactionId(paymentId: Long, transactionId: String): PaymentInfo {
        val payment = paymentRepository.findByIdOrNull(paymentId)
            ?: throw PaymentException.notFound()
        payment.updateTransactionId(transactionId)
        return PaymentInfo.from(payment)
    }

    @Transactional
    fun markRequestFailed(paymentId: Long, reason: String?): PaymentInfo {
        val payment = paymentRepository.findByIdOrNull(paymentId)
            ?: throw PaymentException.notFound()
        payment.markRequestFailed(reason)
        return PaymentInfo.from(payment)
    }

    @Transactional
    fun applyPgResult(paymentId: Long, pgStatus: String, reason: String?, transactionId: String? = null): PaymentInfo {
        if (transactionId != null) {
            paymentRepository.updateTransactionIdIfAbsent(paymentId, transactionId)
        }

        when (pgStatus) {
            PG_STATUS_SUCCESS -> paymentRepository.approveIfNotTerminal(paymentId)
            PG_STATUS_PENDING, PG_STATUS_REQUESTED -> { }
            else -> {
                val updatedCount = paymentRepository.failIfNotTerminal(paymentId, reason ?: "PG 결제 실패")
                if (updatedCount > 0) {
                    cancelOrder(paymentId)
                }
            }
        }

        val payment = paymentRepository.findByIdOrNull(paymentId)
            ?: throw PaymentException.notFound()
        return PaymentInfo.from(payment)
    }

    private fun cancelOrder(paymentId: Long) {
        val payment = paymentRepository.findByIdOrNull(paymentId) ?: return
        val order = orderRepository.findByIdOrNull(payment.orderId) ?: return
        order.cancel()
        log.info("결제 실패로 주문 취소 [paymentId={}, orderId={}]", paymentId, payment.orderId)
    }

    companion object {
        private const val PG_STATUS_SUCCESS = "SUCCESS"
        private const val PG_STATUS_PENDING = "PENDING"
        private const val PG_STATUS_REQUESTED = "REQUESTED"
    }
}
