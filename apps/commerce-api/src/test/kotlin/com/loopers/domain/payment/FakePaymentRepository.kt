package com.loopers.domain.payment

import com.loopers.domain.payment.model.Payment
import com.loopers.domain.payment.model.PaymentStatus
import com.loopers.domain.payment.repository.PaymentRepository
import java.time.ZonedDateTime

class FakePaymentRepository : PaymentRepository {

    private val payments = mutableListOf<Payment>()
    private var sequence = 1L

    override fun save(payment: Payment): Payment {
        return if (payment.id != 0L) {
            payments.removeIf { it.id == payment.id }
            payments.add(payment)
            payment
        } else {
            val saved = Payment.fromPersistence(
                id = sequence++,
                orderId = payment.orderId,
                transactionKey = payment.transactionKey,
                status = payment.status,
                cardType = payment.cardType,
                cardNo = payment.cardNo,
                amount = payment.amount,
                reason = payment.reason,
                createdAt = payment.createdAt,
                updatedAt = payment.updatedAt,
            )
            payments.add(saved)
            saved
        }
    }

    override fun findById(id: Long): Payment? {
        return payments.find { it.id == id }
    }

    override fun findByOrderId(orderId: Long): Payment? {
        return payments.find { it.orderId == orderId }
    }

    override fun findByStatusIn(statuses: List<PaymentStatus>): List<Payment> {
        return payments.filter { it.status in statuses }
    }

    override fun updateStatusConditionally(
        id: Long,
        expectedStatuses: List<PaymentStatus>,
        newStatus: PaymentStatus,
    ): Boolean {
        val payment = payments.find { it.id == id } ?: return false
        if (payment.status !in expectedStatuses) return false
        val updated = Payment.fromPersistence(
            id = payment.id,
            orderId = payment.orderId,
            transactionKey = payment.transactionKey,
            status = newStatus,
            cardType = payment.cardType,
            cardNo = payment.cardNo,
            amount = payment.amount,
            reason = payment.reason,
            createdAt = payment.createdAt,
            updatedAt = ZonedDateTime.now(),
        )
        payments.removeIf { it.id == id }
        payments.add(updated)
        return true
    }
}
