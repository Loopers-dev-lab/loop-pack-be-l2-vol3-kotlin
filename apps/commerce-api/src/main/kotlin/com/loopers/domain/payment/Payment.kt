package com.loopers.domain.payment

class Payment private constructor(
    val persistenceId: Long?,
    val refOrderId: Long,
    val refUserId: Long,
    val transactionKey: String?,
    val cardType: CardType,
    val cardNo: String,
    val amount: Long,
    val status: PaymentStatus,
    val failureReason: String?,
) {

    fun markRequested(transactionKey: String): Payment {
        if (status != PaymentStatus.PENDING) {
            throw PaymentException(
                PaymentError.INVALID_STATUS,
                "PENDING 상태에서만 REQUESTED로 변경할 수 있습니다. 현재 상태: $status",
            )
        }
        return Payment(
            persistenceId = persistenceId,
            refOrderId = refOrderId,
            refUserId = refUserId,
            transactionKey = transactionKey,
            cardType = cardType,
            cardNo = cardNo,
            amount = amount,
            status = PaymentStatus.REQUESTED,
            failureReason = null,
        )
    }

    fun approve(): Payment {
        if (status != PaymentStatus.REQUESTED) {
            throw PaymentException(
                PaymentError.INVALID_STATUS,
                "REQUESTED 상태에서만 승인할 수 있습니다. 현재 상태: $status",
            )
        }
        return Payment(
            persistenceId = persistenceId,
            refOrderId = refOrderId,
            refUserId = refUserId,
            transactionKey = transactionKey,
            cardType = cardType,
            cardNo = cardNo,
            amount = amount,
            status = PaymentStatus.SUCCESS,
            failureReason = null,
        )
    }

    fun fail(reason: String): Payment {
        if (isTerminal()) {
            throw PaymentException(
                PaymentError.INVALID_STATUS,
                "이미 최종 상태입니다. 현재 상태: $status",
            )
        }
        return Payment(
            persistenceId = persistenceId,
            refOrderId = refOrderId,
            refUserId = refUserId,
            transactionKey = transactionKey,
            cardType = cardType,
            cardNo = cardNo,
            amount = amount,
            status = PaymentStatus.FAILED,
            failureReason = reason,
        )
    }

    fun isTerminal(): Boolean = status == PaymentStatus.SUCCESS || status == PaymentStatus.FAILED

    companion object {
        fun create(
            refOrderId: Long,
            refUserId: Long,
            cardType: CardType,
            cardNo: String,
            amount: Long,
        ): Payment {
            require(amount > 0) { "결제 금액은 0보다 커야 합니다." }
            require(cardNo.isNotBlank()) { "카드 번호는 빈 문자열일 수 없습니다." }
            return Payment(
                persistenceId = null,
                refOrderId = refOrderId,
                refUserId = refUserId,
                transactionKey = null,
                cardType = cardType,
                cardNo = cardNo,
                amount = amount,
                status = PaymentStatus.PENDING,
                failureReason = null,
            )
        }

        fun reconstitute(
            persistenceId: Long,
            refOrderId: Long,
            refUserId: Long,
            transactionKey: String?,
            cardType: CardType,
            cardNo: String,
            amount: Long,
            status: PaymentStatus,
            failureReason: String?,
        ): Payment {
            return Payment(
                persistenceId = persistenceId,
                refOrderId = refOrderId,
                refUserId = refUserId,
                transactionKey = transactionKey,
                cardType = cardType,
                cardNo = cardNo,
                amount = amount,
                status = status,
                failureReason = failureReason,
            )
        }
    }
}
