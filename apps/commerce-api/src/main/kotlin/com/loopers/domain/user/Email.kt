package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class Email(val value: String) {
    init {
        if (!Regex("^[^@]+@[^@]+\\.[^@]+$").matches(value)) {
            throw CoreException(ErrorType.BAD_REQUEST, "이메일 형식이 올바르지 않습니다.")
        }
    }
}
