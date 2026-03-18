package com.loopers.domain.payment

import com.loopers.domain.DomainException

class PaymentException(
    val error: PaymentError,
    message: String,
) : DomainException(message)

enum class PaymentError {
    ALREADY_PAID,
    INVALID_STATUS,
    PG_REQUEST_FAILED,
}
