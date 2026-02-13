package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class LoginId(val value: String) {
    companion object {
        private const val MIN_LENGTH = 4
        private const val MAX_LENGTH = 16
    }

    init {
        if (!Regex("^[a-zA-Z0-9]+$").matches(value)) {
            throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 영문 및 숫자만 허용됩니다.")
        }
        if (value.length < MIN_LENGTH) {
            throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 ${MIN_LENGTH}~${MAX_LENGTH}자여야 합니다.")
        }
        if (value.length > MAX_LENGTH) {
            throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 ${MAX_LENGTH}자를 초과할 수 없습니다.")
        }
    }
}
