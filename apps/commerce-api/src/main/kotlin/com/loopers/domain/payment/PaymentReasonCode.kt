package com.loopers.domain.payment

enum class PaymentReasonCode {
    TIMEOUT_UNCERTAIN,
    PG_INTERNAL_ERROR,
    LIMIT_EXCEEDED,
    INVALID_CARD,
    ;

    companion object {
        fun fromPgReason(reason: String?): PaymentReasonCode {
            return when {
                reason == null -> PG_INTERNAL_ERROR
                reason.contains("한도초과") || reason.contains("한도") -> LIMIT_EXCEEDED
                reason.contains("잘못된 카드") || reason.contains("카드") -> INVALID_CARD
                else -> PG_INTERNAL_ERROR
            }
        }
    }
}
