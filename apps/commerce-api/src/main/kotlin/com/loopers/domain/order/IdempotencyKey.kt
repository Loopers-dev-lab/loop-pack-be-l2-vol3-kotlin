package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

data class IdempotencyKey(val value: String) {
    init {
        if (value.isBlank() || value.length > MAX_LENGTH) {
            throw CoreException(ErrorType.ORDER_INVALID_IDEMPOTENCY_KEY)
        }
    }

    companion object {
        private const val MAX_LENGTH = 64
    }
}
