package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

data class UserName(val value: String) {
    val masked: String
        get() = value.dropLast(1) + "*"

    init {
        if (!PATTERN.matches(value)) {
            throw CoreException(ErrorType.USER_INVALID_NAME)
        }
    }

    companion object {
        private val PATTERN = Regex("^[가-힣]+$")
    }
}
