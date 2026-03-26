package com.loopers.domain.payment

import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import java.time.ZonedDateTime

data class PaymentModel(
    val id: Long = 0,
    val orderId: Long,
    val memberId: Long,
    val transactionKey: String? = null,
    val cardType: CardType,
    val cardNo: String,
    val amount: Long,
    val status: PaymentStatus = PaymentStatus.REQUESTED,
    val failReason: String? = null,
    val requestedAt: ZonedDateTime = ZonedDateTime.now(),
    val completedAt: ZonedDateTime? = null,
    val createdAt: ZonedDateTime? = null,
    val updatedAt: ZonedDateTime? = null,
    val deletedAt: ZonedDateTime? = null,
) {
    fun assignTransactionKey(transactionKey: String): PaymentModel {
        if (status != PaymentStatus.REQUESTED) {
            throw CoreException(ErrorType.BAD_REQUEST, "REQUESTED 상태에서만 transactionKey를 할당할 수 있습니다.")
        }
        return copy(status = PaymentStatus.PENDING, transactionKey = transactionKey)
    }

    fun markSuccess(): PaymentModel {
        if (status != PaymentStatus.PENDING) {
            throw CoreException(ErrorType.BAD_REQUEST, "PENDING 상태에서만 성공 처리할 수 있습니다.")
        }
        return copy(status = PaymentStatus.SUCCESS, completedAt = ZonedDateTime.now())
    }

    fun markFailed(reason: String?): PaymentModel {
        if (status != PaymentStatus.REQUESTED && status != PaymentStatus.PENDING) {
            throw CoreException(ErrorType.BAD_REQUEST, "REQUESTED 또는 PENDING 상태에서만 실패 처리할 수 있습니다.")
        }
        return copy(status = PaymentStatus.FAILED, failReason = reason, completedAt = ZonedDateTime.now())
    }

    fun isTerminal(): Boolean = status == PaymentStatus.SUCCESS || status == PaymentStatus.FAILED

    fun isActive(): Boolean = status == PaymentStatus.REQUESTED || status == PaymentStatus.PENDING
}
