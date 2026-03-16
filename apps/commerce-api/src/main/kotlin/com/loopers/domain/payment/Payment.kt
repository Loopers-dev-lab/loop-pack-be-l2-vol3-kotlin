package com.loopers.domain.payment

import java.time.ZonedDateTime

class Payment(
    val id: Long? = null,
    val orderId: Long,
    val memberId: Long,
    val cardType: CardType,
    val cardNo: String,
    val amount: Long,
    requestedAt: ZonedDateTime = ZonedDateTime.now(),
    status: PaymentStatus = PaymentStatus.REQUESTED,
    pgTransactionKey: String? = null,
    reason: String? = null,
) {
    var requestedAt: ZonedDateTime = requestedAt
        private set

    var status: PaymentStatus = status
        private set

    var pgTransactionKey: String? = pgTransactionKey
        private set

    var reason: String? = reason
        private set

    fun markAccepted(transactionKey: String, reason: String?) {
        pgTransactionKey = transactionKey
        status = PaymentStatus.PENDING
        this.reason = reason
    }

    fun markRequestFailed(reason: String) {
        status = PaymentStatus.REQUEST_FAILED
        this.reason = reason
    }

    fun markUnknown(reason: String) {
        status = PaymentStatus.UNKNOWN
        this.reason = reason
    }

    fun applyPgResult(
        transactionKey: String?,
        status: PgPaymentStatus,
        reason: String?,
    ) {
        if (this.status == PaymentStatus.SUCCESS || this.status == PaymentStatus.FAILED) {
            return
        }
        transactionKey?.let { pgTransactionKey = it }
        this.reason = reason
        this.status = when (status) {
            PgPaymentStatus.PENDING -> PaymentStatus.PENDING
            PgPaymentStatus.SUCCESS -> PaymentStatus.SUCCESS
            PgPaymentStatus.FAILED -> PaymentStatus.FAILED
        }
    }

    fun isRecoverable(): Boolean = status == PaymentStatus.PENDING || status == PaymentStatus.UNKNOWN
}
