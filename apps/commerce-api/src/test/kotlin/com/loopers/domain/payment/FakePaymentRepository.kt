package com.loopers.domain.payment

import com.loopers.domain.BaseEntity
import java.time.ZonedDateTime

class FakePaymentRepository : PaymentRepository {

    private val store = mutableListOf<Payment>()
    private var idSequence = 1L

    override fun save(payment: Payment): Payment {
        if (payment.id == 0L) {
            setEntityId(payment, idSequence++)
            initTimestamps(payment)
            store.add(payment)
        } else {
            // 이미 존재하면 교체 (JPA merge 동작과 일치)
            store.removeAll { it.id == payment.id }
            store.add(payment)
        }
        return payment
    }

    override fun findById(id: Long): Payment? {
        return store.find { it.id == id && it.deletedAt == null }
    }

    override fun findByTransactionKey(transactionKey: String): Payment? {
        return store.find { it.transactionKey == transactionKey && it.deletedAt == null }
    }

    override fun findByOrderId(orderId: Long): List<Payment> {
        return store.filter { it.orderId == orderId && it.deletedAt == null }
    }

    override fun findByPaymentStatusIn(statuses: List<PaymentStatus>): List<Payment> {
        return store.filter { it.paymentStatus in statuses && it.deletedAt == null }
    }

    private fun setEntityId(entity: BaseEntity, id: Long) {
        val idField = BaseEntity::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
    }

    private fun initTimestamps(entity: BaseEntity) {
        val now = ZonedDateTime.now()
        val createdAtField = BaseEntity::class.java.getDeclaredField("createdAt")
        createdAtField.isAccessible = true
        createdAtField.set(entity, now)
        val updatedAtField = BaseEntity::class.java.getDeclaredField("updatedAt")
        updatedAtField.isAccessible = true
        updatedAtField.set(entity, now)
    }
}
