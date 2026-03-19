package com.loopers.domain.payment

import com.loopers.domain.common.vo.OrderId
import com.loopers.domain.payment.model.Payment
import com.loopers.domain.payment.model.PaymentStatus
import com.loopers.domain.payment.repository.PaymentRepository

class FakePaymentRepository : PaymentRepository {

    private val payments = mutableListOf<Payment>()
    private var sequence = 1L

    override fun save(payment: Payment): Payment {
        val copy = Payment.fromPersistence(
            id = if (payment.id != 0L) payment.id else sequence++,
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
        payments.removeIf { it.id == copy.id }
        payments.add(copy)
        return copy
    }

    override fun findById(id: Long): Payment? {
        return payments.find { it.id == id }?.toCopy()
    }

    override fun findByOrderId(orderId: OrderId): Payment? {
        return payments.filter { it.orderId == orderId.value }.maxByOrNull { it.id }?.toCopy()
    }

    override fun findByIdForUpdate(id: Long): Payment? {
        return payments.find { it.id == id }?.toCopy()
    }

    override fun findByOrderIdForUpdate(orderId: OrderId): Payment? {
        return payments.filter { it.orderId == orderId.value }.maxByOrNull { it.id }?.toCopy()
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
