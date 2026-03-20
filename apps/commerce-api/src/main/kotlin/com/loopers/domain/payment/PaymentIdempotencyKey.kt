package com.loopers.domain.payment

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

data class PaymentIdempotencyKey(val value: String) {
    init {
        if (value.isBlank() || value.length > MAX_LENGTH) {
            throw CoreException(ErrorType.PAYMENT_INVALID_IDEMPOTENCY_KEY)
        }
    }

    companion object {
        private const val MAX_LENGTH = 64
    }
}
