package com.loopers.domain.payment

import com.loopers.domain.common.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.time.ZonedDateTime

class Payment private constructor(
    val id: Long?,
    val orderId: Long,
    val userId: Long,
    val idempotencyKey: PaymentIdempotencyKey,
    val status: Status,
    val cardType: String,
    val maskedCardNo: String,
    val amount: Money,
    val transactionKey: String?,
    val reasonCode: PaymentReasonCode?,
    val requestFingerprint: String,
    val createdAt: ZonedDateTime?,
) {
    init {
        when (status) {
            Status.SUCCESS -> {
                if (transactionKey.isNullOrBlank()) {
                    throw CoreException(ErrorType.PAYMENT_INVALID_STATUS_TRANSITION)
                }
            }
            Status.FAILED -> {
                if (reasonCode == null) {
                    throw CoreException(ErrorType.PAYMENT_INVALID_STATUS_TRANSITION)
                }
            }
            Status.PENDING -> Unit
        }
    }

    fun succeed(transactionKey: String): Payment {
        if (status != Status.PENDING) {
            throw CoreException(ErrorType.PAYMENT_INVALID_STATUS_TRANSITION)
        }
        return Payment(
            id = id,
            orderId = orderId,
            userId = userId,
            idempotencyKey = idempotencyKey,
            status = Status.SUCCESS,
            cardType = cardType,
            maskedCardNo = maskedCardNo,
            amount = amount,
            transactionKey = transactionKey,
            reasonCode = null,
            requestFingerprint = requestFingerprint,
            createdAt = createdAt,
        )
    }

    fun fail(reasonCode: PaymentReasonCode): Payment {
        if (status != Status.PENDING) {
            throw CoreException(ErrorType.PAYMENT_INVALID_STATUS_TRANSITION)
        }
        return Payment(
            id = id,
            orderId = orderId,
            userId = userId,
            idempotencyKey = idempotencyKey,
            status = Status.FAILED,
            cardType = cardType,
            maskedCardNo = maskedCardNo,
            amount = amount,
            transactionKey = transactionKey,
            reasonCode = reasonCode,
            requestFingerprint = requestFingerprint,
            createdAt = createdAt,
        )
    }

    fun updateTransactionKey(transactionKey: String): Payment {
        return Payment(
            id = id,
            orderId = orderId,
            userId = userId,
            idempotencyKey = idempotencyKey,
            status = status,
            cardType = cardType,
            maskedCardNo = maskedCardNo,
            amount = amount,
            transactionKey = transactionKey,
            reasonCode = reasonCode,
            requestFingerprint = requestFingerprint,
            createdAt = createdAt,
        )
    }

    fun applyTimeoutFallback(): Payment {
        return Payment(
            id = id,
            orderId = orderId,
            userId = userId,
            idempotencyKey = idempotencyKey,
            status = Status.PENDING,
            cardType = cardType,
            maskedCardNo = maskedCardNo,
            amount = amount,
            transactionKey = null,
            reasonCode = PaymentReasonCode.TIMEOUT_UNCERTAIN,
            requestFingerprint = requestFingerprint,
            createdAt = createdAt,
        )
    }

    val isTerminal: Boolean
        get() = status == Status.SUCCESS || status == Status.FAILED

    enum class Status {
        PENDING,
        SUCCESS,
        FAILED,
    }

    companion object {
        fun create(
            orderId: Long,
            userId: Long,
            idempotencyKey: PaymentIdempotencyKey,
            cardType: String,
            maskedCardNo: String,
            amount: Money,
            requestFingerprint: String,
        ): Payment = Payment(
            id = null,
            orderId = orderId,
            userId = userId,
            idempotencyKey = idempotencyKey,
            status = Status.PENDING,
            cardType = cardType,
            maskedCardNo = maskedCardNo,
            amount = amount,
            transactionKey = null,
            reasonCode = null,
            requestFingerprint = requestFingerprint,
            createdAt = null,
        )

        fun retrieve(
            id: Long,
            orderId: Long,
            userId: Long,
            idempotencyKey: PaymentIdempotencyKey,
            status: Status,
            cardType: String,
            maskedCardNo: String,
            amount: Money,
            transactionKey: String?,
            reasonCode: PaymentReasonCode?,
            requestFingerprint: String,
            createdAt: ZonedDateTime,
        ): Payment = Payment(
            id = id,
            orderId = orderId,
            userId = userId,
            idempotencyKey = idempotencyKey,
            status = status,
            cardType = cardType,
            maskedCardNo = maskedCardNo,
            amount = amount,
            transactionKey = transactionKey,
            reasonCode = reasonCode,
            requestFingerprint = requestFingerprint,
            createdAt = createdAt,
        )
    }
}
