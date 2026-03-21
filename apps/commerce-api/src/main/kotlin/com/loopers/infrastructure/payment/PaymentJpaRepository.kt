package com.loopers.infrastructure.payment

import com.loopers.domain.payment.Payment
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.ZonedDateTime

interface PaymentJpaRepository : JpaRepository<PaymentEntity, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): PaymentEntity?
    fun findByIdempotencyKeyAndDeletedAtIsNull(idempotencyKey: String): PaymentEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentEntity p WHERE p.idempotencyKey = :idempotencyKey AND p.deletedAt IS NULL")
    fun findByIdempotencyKeyForUpdate(idempotencyKey: String): PaymentEntity?

    fun findByOrderIdAndStatusAndDeletedAtIsNull(orderId: Long, status: Payment.Status): PaymentEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentEntity p WHERE p.orderId = :orderId AND p.status = :status AND p.deletedAt IS NULL")
    fun findByOrderIdAndStatusForUpdate(orderId: Long, status: Payment.Status): PaymentEntity?
    fun findAllByOrderIdAndDeletedAtIsNull(orderId: Long): List<PaymentEntity>
    fun findAllByStatusAndCreatedAtBeforeAndDeletedAtIsNull(
        status: Payment.Status,
        createdAt: ZonedDateTime,
    ): List<PaymentEntity>

    @Modifying
    @Query(
        """
        UPDATE PaymentEntity p
           SET p.status = :status,
               p.transactionKey = :transactionKey,
               p.reasonCode = :reasonCode,
               p.updatedAt = :updatedAt
         WHERE p.id = :id
           AND p.status = com.loopers.domain.payment.Payment.Status.PENDING
           AND p.deletedAt IS NULL
        """,
    )
    fun updateIfPending(
        @Param("id") id: Long,
        @Param("status") status: Payment.Status,
        @Param("transactionKey") transactionKey: String?,
        @Param("reasonCode") reasonCode: com.loopers.domain.payment.PaymentReasonCode?,
        @Param("updatedAt") updatedAt: ZonedDateTime,
    ): Int
}
