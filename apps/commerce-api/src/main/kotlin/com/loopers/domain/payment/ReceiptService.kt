package com.loopers.domain.payment

import com.loopers.application.api.payment.dto.PaymentCallbackCommand
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZonedDateTime

@Service
@Transactional(readOnly = true)
class ReceiptService(
    private val receiptRepository: ReceiptRepository,
) {

    fun getReceiptByOrderId(orderId: Long): Receipt? =
        receiptRepository.findByOrderId(orderId)

    fun getReceiptByOrderIdForUpdate(orderId: Long): Receipt? =
        receiptRepository.findByOrderIdForUpdate(orderId)

    fun validateReceiptForNewPayment(receipt: Receipt) {
        when (receipt.status) {
            ReceiptStatus.PENDING -> {
                throw CoreException(ErrorType.CONFLICT, "이미 이 주문에 대한 결제가 진행 중입니다")
            }
            ReceiptStatus.COMPLETED -> {
                throw CoreException(ErrorType.CONFLICT, "이미 이 주문에 대한 결제가 완료되었습니다")
            }
            ReceiptStatus.CANCELLED -> {
                throw CoreException(ErrorType.CONFLICT, "이미 취소된 결제입니다")
            }
            ReceiptStatus.TIMEOUT, ReceiptStatus.FAILED -> {
                // 재시도 가능
            }
        }
    }

    fun getReceiptByTransactionIdForUpdate(transactionId: String): Receipt =
        receiptRepository.findByTransactionIdForUpdate(transactionId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "결제 정보가 존재하지 않습니다")

    @Transactional
    fun save(receipt: Receipt): Receipt =
        receiptRepository.save(receipt)

    fun findPendingReceiptsOlderThan(before: ZonedDateTime): List<Receipt> =
        receiptRepository.findByStatusAndCreatedAtBefore(ReceiptStatus.PENDING, before)

    @Transactional
    fun initiateReceipt(
        orderId: Long,
        transactionId: String,
        amount: BigDecimal,
        cardType: String,
        cardNo: String,
    ): Receipt {
        val receipt = Receipt.create(orderId, transactionId, amount, cardType, cardNo)
        return receiptRepository.save(receipt)
    }

    @Transactional
    fun updateReceiptStatus(command: PaymentCallbackCommand): Receipt {
        val receipt = getReceiptByTransactionIdForUpdate(command.transactionId)

        // ✅ orderId 검증: 콜백의 orderId와 Receipt의 orderId가 일치하는지 확인
        if (receipt.orderId != command.orderId) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 ID 불일치. Receipt: ${receipt.orderId}, Callback: ${command.orderId}")
        }

        // 멱등성: PENDING 상태만 처리
        if (receipt.status != ReceiptStatus.PENDING) {
            return receipt
        }

        // PG 콜백 status에 따라 분기 처리
        when (command.status?.uppercase()) {
            "FAILED" -> receipt.markAsFailed()
            "CANCELLED" -> receipt.markAsCancelled()
            "COMPLETED" -> receipt.markAsCompleted(command.amount)
            else -> throw CoreException(ErrorType.BAD_REQUEST, "알 수 없는 결제 상태: ${command.status}")
        }

        return receipt
    }

    /**
     * 복구 대상인 Receipt (PENDING, FAILED, TIMEOUT)을 조회합니다.
     * (생성 후 지정된 시간 이상 경과한 것만)
     *
     * @param delayMinutes 최소 경과 시간 (분)
     * @return 복구 대상 Receipt 목록
     */
    fun getReceiptsForRecovery(delayMinutes: Long): List<Receipt> {
        val threshold = LocalDateTime.now().minusMinutes(delayMinutes)
        val statuses = listOf(ReceiptStatus.PENDING, ReceiptStatus.FAILED, ReceiptStatus.TIMEOUT)
        return receiptRepository.findReceiptsForRecovery(statuses, threshold)
    }

    /**
     * Receipt을 완료 상태로 표시합니다. (복구용)
     *
     * 멱등성: 이미 COMPLETED면 무시, PENDING/FAILED/TIMEOUT만 처리
     */
    @Transactional
    fun markAsCompleted(receipt: Receipt) {
        val lockedReceipt = receiptRepository.findByIdForUpdate(receipt.id!!)
            ?: throw CoreException(ErrorType.NOT_FOUND, "Receipt not found")

        // ✅ 멱등성: 이미 COMPLETED면 무시
        if (lockedReceipt.status == ReceiptStatus.COMPLETED) {
            return
        }

        // PENDING/FAILED/TIMEOUT만 처리
        if (lockedReceipt.status !in
            listOf(
                ReceiptStatus.PENDING,
                ReceiptStatus.FAILED,
                ReceiptStatus.TIMEOUT,
            )
        ) {
            return // 예외 대신 무시 (멱등성)
        }

        lockedReceipt.markAsCompleted(receipt.amount)
    }

    /**
     * Receipt을 실패 상태로 표시합니다. (복구용)
     */
    @Transactional
    fun markAsFailed(receipt: Receipt, reason: String?) {
        val lockedReceipt = receiptRepository.findByIdForUpdate(receipt.id!!)
            ?: throw CoreException(ErrorType.NOT_FOUND, "Receipt not found")

        // 멱등성: PENDING 상태만 처리
        if (lockedReceipt.status != ReceiptStatus.PENDING) {
            return
        }

        lockedReceipt.markAsFailed()
    }

    /**
     * Receipt을 취소 상태로 표시합니다. (복구용)
     */
    @Transactional
    fun markAsCancelled(receipt: Receipt, reason: String?) {
        val lockedReceipt = receiptRepository.findByIdForUpdate(receipt.id!!)
            ?: throw CoreException(ErrorType.NOT_FOUND, "Receipt not found")

        // 멱등성: PENDING 상태만 처리
        if (lockedReceipt.status != ReceiptStatus.PENDING) {
            return
        }

        lockedReceipt.markAsCancelled()
    }

    /**
     * Receipt을 타임아웃 상태로 표시합니다. (네트워크 타임아웃용)
     */
    @Transactional
    fun markAsTimeout(receipt: Receipt) {
        val lockedReceipt = receiptRepository.findByIdForUpdate(receipt.id!!)
            ?: throw CoreException(ErrorType.NOT_FOUND, "Receipt not found")

        // 멱등성: PENDING 상태만 처리
        if (lockedReceipt.status != ReceiptStatus.PENDING) {
            return
        }

        lockedReceipt.markAsTimeout()
    }
}
