package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

data class LoginId(val value: String) {
    init {
        if (!PATTERN.matches(value)) {
            throw CoreException(ErrorType.USER_INVALID_LOGIN_ID)
        }
    }

    companion object {
        private val PATTERN = Regex("^[a-zA-Z0-9]+$")
    }
}
