package com.loopers.domain.payment.model

enum class PaymentStatus {
    REQUESTED,
    SUCCESS,
    FAILED,
    RECOVERY_FAILED,
    TIMEOUT,
}
