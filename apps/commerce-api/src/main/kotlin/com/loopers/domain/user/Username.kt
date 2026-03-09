package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class Username private constructor(
    val value: String,
) {
    companion object {
        private val USERNAME_REGEX = Regex("^[a-zA-Z0-9]+$")

        fun of(value: String): Username {
            if (value.isBlank()) {
                throw CoreException(ErrorType.BAD_REQUEST, "아이디는 비어있을 수 없습니다.")
            }
            if (!USERNAME_REGEX.matches(value)) {
                throw CoreException(ErrorType.BAD_REQUEST, "아이디는 영문과 숫자만 사용할 수 있습니다.")
            }
            return Username(value)
        }
    }
}
