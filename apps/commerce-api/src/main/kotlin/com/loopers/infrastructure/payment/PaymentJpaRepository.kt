package com.loopers.infrastructure.payment

import com.loopers.domain.payment.PaymentStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.time.ZonedDateTime

interface PaymentJpaRepository : JpaRepository<PaymentJpaModel, Long> {
    fun findByTransactionKey(transactionKey: String): PaymentJpaModel?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentJpaModel p WHERE p.transactionKey = :transactionKey")
    fun findByTransactionKeyWithLock(transactionKey: String): PaymentJpaModel?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentJpaModel p WHERE p.id = :id")
    fun findByIdWithLock(id: Long): PaymentJpaModel?

    fun findAllByOrderId(orderId: Long): List<PaymentJpaModel>

    fun findAllByStatusAndRequestedAtBefore(status: PaymentStatus, before: ZonedDateTime): List<PaymentJpaModel>
}
