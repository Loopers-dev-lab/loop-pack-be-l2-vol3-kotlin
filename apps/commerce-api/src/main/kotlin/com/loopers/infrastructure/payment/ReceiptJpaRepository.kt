package com.loopers.infrastructure.payment

import com.loopers.domain.payment.Receipt
import com.loopers.domain.payment.ReceiptStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime
import java.time.ZonedDateTime

interface ReceiptJpaRepository : JpaRepository<Receipt, Long> {
    fun findByTransactionId(transactionId: String): Receipt?

    fun findByOrderId(orderId: Long): Receipt?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Receipt r WHERE r.transactionId = :transactionId")
    fun findByTransactionIdForUpdate(transactionId: String): Receipt?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Receipt r WHERE r.orderId = :orderId")
    fun findByOrderIdForUpdate(orderId: Long): Receipt?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Receipt r WHERE r.id = :id")
    fun findByIdForUpdate(id: Long): Receipt?

    @Query("SELECT r FROM Receipt r WHERE r.status = :status AND r.createdAt < :before")
    fun findByStatusAndCreatedAtBefore(status: ReceiptStatus, before: ZonedDateTime): List<Receipt>

    @Query("SELECT r FROM Receipt r WHERE r.status = 'PENDING' AND r.createdAt < :before")
    fun findPendingReceiptsCreatedBefore(before: LocalDateTime): List<Receipt>

    @Query("SELECT r FROM Receipt r WHERE r.status IN :statuses AND r.createdAt < :before")
    fun findReceiptsForRecovery(statuses: List<ReceiptStatus>, before: LocalDateTime): List<Receipt>
}
