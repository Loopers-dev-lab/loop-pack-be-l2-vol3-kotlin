package com.loopers.domain.payment

import com.loopers.domain.payment.model.Payment
import com.loopers.domain.payment.model.PaymentStatus
import com.loopers.domain.payment.repository.PaymentRepository

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
        return payments.find { it.id == id }?.toCopy()
    }

    override fun findByOrderId(orderId: Long): Payment? {
        return payments.find { it.orderId == orderId }?.toCopy()
    }

    override fun findByOrderIdForUpdate(orderId: Long): Payment? {
        return payments.find { it.orderId == orderId }?.toCopy()
    }

    override fun findByStatusIn(statuses: List<PaymentStatus>, limit: Int): List<Payment> {
        return payments.filter { it.status in statuses }.take(limit).map { it.toCopy() }
    }

    private fun Payment.toCopy(): Payment = Payment.fromPersistence(
        id = id,
        orderId = orderId,
        transactionKey = transactionKey,
        status = status,
        cardType = cardType,
        cardNo = cardNo,
        amount = amount,
        reason = reason,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
