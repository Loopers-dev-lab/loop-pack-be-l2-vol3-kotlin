package com.loopers.infrastructure.payment

import com.loopers.domain.payment.Receipt
import com.loopers.domain.payment.ReceiptRepository
import org.springframework.stereotype.Repository

@Repository
class ReceiptRepositoryImpl(
    private val receiptJpaRepository: ReceiptJpaRepository,
) : ReceiptRepository {

    override fun findByTransactionId(transactionId: String): Receipt? =
        receiptJpaRepository.findByTransactionId(transactionId)

    override fun findByOrderId(orderId: Long): Receipt? =
        receiptJpaRepository.findByOrderId(orderId)

    override fun findByTransactionIdForUpdate(transactionId: String): Receipt? =
        receiptJpaRepository.findByTransactionIdForUpdate(transactionId)

    override fun findByOrderIdForUpdate(orderId: Long): Receipt? =
        receiptJpaRepository.findByOrderIdForUpdate(orderId)

    override fun save(receipt: Receipt): Receipt =
        receiptJpaRepository.save(receipt)
}
