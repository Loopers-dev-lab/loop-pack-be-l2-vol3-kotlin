package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class Email private constructor(
    val value: String,
) {
    companion object {
        private val EMAIL_REGEX =
            Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

        fun of(value: String): Email {
            if (value.isBlank()) {
                throw CoreException(ErrorType.BAD_REQUEST, "이메일은 비어있을 수 없습니다.")
            }
            if (!EMAIL_REGEX.matches(value)) {
                throw CoreException(ErrorType.BAD_REQUEST, "이메일 형식이 올바르지 않습니다.")
            }
            return Email(value)
        }
    }
}
