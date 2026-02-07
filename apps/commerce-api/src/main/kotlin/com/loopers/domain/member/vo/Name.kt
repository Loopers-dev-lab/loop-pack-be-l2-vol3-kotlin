package com.loopers.domain.member.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

data class Name(val value: String) {
    init {
        if (value.isBlank()) {
            throw CoreException(ErrorType.INVALID_NAME_FORMAT)
        }
    }

    fun masked(): String {
        return if (value.length <= 1) {
            "*"
        } else {
            value.substring(0, value.length - 1) + "*"
        }
    }
}
