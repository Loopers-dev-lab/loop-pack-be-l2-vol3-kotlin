package com.loopers.domain.user.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

@JvmInline
value class Name(val value: String) {
    companion object {
        private const val MAX_LENGTH = 10
    }

    init {
        if (value.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "이름은 필수입니다.")
        }
        if (value.length !in 1..MAX_LENGTH) {
            throw CoreException(ErrorType.BAD_REQUEST, "이름은 ${MAX_LENGTH}자 이내여야 합니다.")
        }
        if (!Regex("^[가-힣a-zA-Z]+$").matches(value)) {
            throw CoreException(ErrorType.BAD_REQUEST, "이름은 한글 또는 영문만 허용됩니다.")
        }
    }

    fun masked(): String {
        if (value.length == 1) return "*"
        return value.dropLast(1) + "*"
    }
}
