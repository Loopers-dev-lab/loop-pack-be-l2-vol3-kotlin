package com.loopers.infrastructure.payment

import com.loopers.domain.payment.PaymentStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.time.ZonedDateTime

interface PaymentJpaRepository : JpaRepository<PaymentEntity, Long> {
    fun findByTransactionKey(transactionKey: String): PaymentEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentEntity p WHERE p.transactionKey = :transactionKey")
    fun findByTransactionKeyForUpdate(transactionKey: String): PaymentEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentEntity p WHERE p.id = :id")
    fun findByIdForUpdate(id: Long): PaymentEntity?

    fun findByOrderId(orderId: Long): PaymentEntity?

    fun findAllByStatusAndCreatedAtBefore(status: PaymentStatus, createdAt: ZonedDateTime): List<PaymentEntity>
    fun findAllByStatusInAndCreatedAtBefore(statuses: List<PaymentStatus>, createdAt: ZonedDateTime): List<PaymentEntity>
}
