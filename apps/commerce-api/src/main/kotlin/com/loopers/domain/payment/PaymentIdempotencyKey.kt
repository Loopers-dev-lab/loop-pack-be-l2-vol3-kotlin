package com.loopers.domain.payment

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.util.UUID

data class PaymentIdempotencyKey(val value: String) {
    init {
        if (value.isBlank() || !isValidUuid(value)) {
            throw CoreException(ErrorType.PAYMENT_INVALID_IDEMPOTENCY_KEY)
        }
    }

    companion object {
        private fun isValidUuid(value: String): Boolean = try {
            UUID.fromString(value)
            true
        } catch (_: IllegalArgumentException) {
            false
        }
    }
}
