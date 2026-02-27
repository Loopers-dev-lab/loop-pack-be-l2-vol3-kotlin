package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

data class RawPassword(val value: String) {
    init {
        if (!PATTERN.matches(value)) {
            throw CoreException(ErrorType.USER_INVALID_PASSWORD)
        }
    }

    companion object {
        private val PATTERN = Regex("^[a-zA-Z0-9!@#\$%^&*()_+\\-=\\[\\]{}|;:',.<>?/]+$")
    }
}
