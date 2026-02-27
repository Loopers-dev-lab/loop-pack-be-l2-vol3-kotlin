package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

data class Email(val value: String) {
    init {
        if (!PATTERN.matches(value)) {
            throw CoreException(ErrorType.USER_INVALID_EMAIL)
        }
    }

    companion object {
        private val PATTERN = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
    }
}
