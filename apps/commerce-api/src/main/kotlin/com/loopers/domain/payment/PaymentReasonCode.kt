package com.loopers.domain.payment

enum class PaymentReasonCode {
    TIMEOUT_UNCERTAIN,
    PG_INTERNAL_ERROR,
    LIMIT_EXCEEDED,
    INVALID_CARD,
}
