package com.loopers.domain.payment.model

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.time.ZonedDateTime

class Payment private constructor(
    val id: Long = 0,
    val orderId: Long,
    transactionKey: String?,
    status: PaymentStatus,
    val cardType: CardType,
    val cardNo: String,
    val amount: Long,
    reason: String? = null,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
) {
    var transactionKey: String? = transactionKey
        private set

    var status: PaymentStatus = status
        private set

    var reason: String? = reason
        private set

    fun markSuccess(transactionKey: String) {
        if (status != PaymentStatus.REQUESTED && status != PaymentStatus.TIMEOUT) {
            throw CoreException(ErrorType.BAD_REQUEST, "REQUESTED 또는 TIMEOUT 상태에서만 성공 처리할 수 있습니다.")
        }
        this.transactionKey = transactionKey
        this.status = PaymentStatus.SUCCESS
    }

    fun markFailed(reason: String) {
        if (status != PaymentStatus.REQUESTED && status != PaymentStatus.TIMEOUT) {
            throw CoreException(ErrorType.BAD_REQUEST, "REQUESTED 또는 TIMEOUT 상태에서만 실패 처리할 수 있습니다.")
        }
        this.reason = reason
        this.status = PaymentStatus.FAILED
    }

    fun markTimeout() {
        if (status != PaymentStatus.REQUESTED) {
            throw CoreException(ErrorType.BAD_REQUEST, "REQUESTED 상태에서만 타임아웃 처리할 수 있습니다.")
        }
        this.status = PaymentStatus.TIMEOUT
    }

    companion object {
        private fun maskCardNo(cardNo: String): String {
            val parts = cardNo.split("-")
            if (parts.size != 4) return cardNo
            return "${parts[0]}-****-****-${parts[3]}"
        }

        fun create(
            orderId: Long,
            cardType: CardType,
            cardNo: String,
            amount: Long,
        ): Payment {
            val now = ZonedDateTime.now()
            return Payment(
                orderId = orderId,
                transactionKey = null,
                status = PaymentStatus.REQUESTED,
                cardType = cardType,
                cardNo = maskCardNo(cardNo),
                amount = amount,
                createdAt = now,
                updatedAt = now,
            )
        }

        fun fromPersistence(
            id: Long,
            orderId: Long,
            transactionKey: String?,
            status: PaymentStatus,
            cardType: CardType,
            cardNo: String,
            amount: Long,
            reason: String?,
            createdAt: ZonedDateTime,
            updatedAt: ZonedDateTime,
        ): Payment {
            return Payment(
                id = id,
                orderId = orderId,
                transactionKey = transactionKey,
                status = status,
                cardType = cardType,
                cardNo = cardNo,
                amount = amount,
                reason = reason,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
        }
    }
}
