package com.loopers.infrastructure.payment

import com.loopers.domain.payment.Receipt
import com.loopers.domain.payment.ReceiptRepository
import com.loopers.domain.payment.ReceiptStatus
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZonedDateTime

@Repository
class ReceiptRepositoryImpl(
    private val receiptJpaRepository: ReceiptJpaRepository,
) : ReceiptRepository {

    override fun findById(id: Long): Receipt? =
        receiptJpaRepository.findById(id).orElse(null)

    override fun findByTransactionId(transactionId: String): Receipt? =
        receiptJpaRepository.findByTransactionId(transactionId)

    override fun findByOrderId(orderId: Long): Receipt? =
        receiptJpaRepository.findByOrderId(orderId)

    override fun findByTransactionIdForUpdate(transactionId: String): Receipt? =
        receiptJpaRepository.findByTransactionIdForUpdate(transactionId)

    override fun findByOrderIdForUpdate(orderId: Long): Receipt? =
        receiptJpaRepository.findByOrderIdForUpdate(orderId)

    override fun findByIdForUpdate(id: Long): Receipt? =
        receiptJpaRepository.findByIdForUpdate(id)

    override fun findByStatusAndCreatedAtBefore(status: ReceiptStatus, before: ZonedDateTime): List<Receipt> =
        receiptJpaRepository.findByStatusAndCreatedAtBefore(status, before)

    override fun findPendingReceiptsCreatedBefore(before: LocalDateTime): List<Receipt> =
        receiptJpaRepository.findPendingReceiptsCreatedBefore(before)

    override fun findReceiptsForRecovery(statuses: List<ReceiptStatus>, before: LocalDateTime): List<Receipt> =
        receiptJpaRepository.findReceiptsForRecovery(statuses, before)

    override fun save(receipt: Receipt): Receipt =
        receiptJpaRepository.save(receipt)
}
