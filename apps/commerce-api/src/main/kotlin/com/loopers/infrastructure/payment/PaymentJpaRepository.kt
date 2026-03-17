package com.loopers.infrastructure.payment

import com.loopers.domain.payment.Payment
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface PaymentJpaRepository : JpaRepository<Payment, Long> {
    fun findByTransactionId(transactionId: String): Payment?

    fun findByOrderId(orderId: Long): Payment?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.transactionId = :transactionId")
    fun findByTransactionIdForUpdate(transactionId: String): Payment?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.orderId = :orderId")
    fun findByOrderIdForUpdate(orderId: Long): Payment?
}
