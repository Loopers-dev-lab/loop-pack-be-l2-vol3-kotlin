package com.loopers.domain.member.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

data class Email(val value: String) {
    init {
        if (value.isBlank() || !value.matches(PATTERN)) {
            throw CoreException(ErrorType.INVALID_EMAIL_FORMAT)
        }
    }

    companion object {
        private val PATTERN = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
    }
}
