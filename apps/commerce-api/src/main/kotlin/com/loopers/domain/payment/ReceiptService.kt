package com.loopers.domain.payment

import com.loopers.application.api.payment.dto.PaymentCallbackCommand
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.ZonedDateTime

@Service
@Transactional(readOnly = true)
class ReceiptService(
    private val receiptRepository: ReceiptRepository,
) {

    fun getReceiptByTransactionId(transactionId: String): Receipt =
        receiptRepository.findByTransactionId(transactionId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "결제 정보가 존재하지 않습니다")

    fun getReceiptByOrderId(orderId: Long): Receipt? =
        receiptRepository.findByOrderId(orderId)

    fun getReceiptByTransactionIdForUpdate(transactionId: String): Receipt =
        receiptRepository.findByTransactionIdForUpdate(transactionId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "결제 정보가 존재하지 않습니다")

    @Transactional
    fun save(receipt: Receipt): Receipt =
        receiptRepository.save(receipt)

    @Transactional
    fun markAsPending(receiptId: Long) {
        val receipt = receiptRepository.findById(receiptId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "결제 정보가 존재하지 않습니다")
        receipt.markAsPending()
        receiptRepository.save(receipt)
    }

    fun findPendingReceiptsOlderThan(before: ZonedDateTime): List<Receipt> =
        receiptRepository.findByStatusAndCreatedAtBefore(ReceiptStatus.PENDING, before)

    @Transactional
    fun initiateReceipt(
        orderId: Long,
        transactionId: String,
        amount: BigDecimal,
        cardType: String = "",
        cardNo: String = "",
    ): Receipt {
        val receipt = Receipt.create(orderId, transactionId, amount, cardType, cardNo)
        return receiptRepository.save(receipt)
    }

    @Transactional
    fun updateReceiptStatus(command: PaymentCallbackCommand) {
        val receipt = getReceiptByTransactionIdForUpdate(command.transactionId)

        // 멱등성: INITIATED 또는 PENDING 상태만 처리
        if (receipt.status !in listOf(ReceiptStatus.INITIATED, ReceiptStatus.PENDING)) {
            return
        }

        // PG 콜백 status에 따라 분기 처리
        when (command.status?.uppercase()) {
            "FAILED" -> receipt.markAsFailed()
            "CANCELLED" -> receipt.markAsCancelled()
            "COMPLETED" -> receipt.markAsCompleted(command.amount)
            else -> throw CoreException(
                ErrorType.BAD_REQUEST,
                "알 수 없는 결제 상태: ${command.status}",
            )
        }
    }
}
