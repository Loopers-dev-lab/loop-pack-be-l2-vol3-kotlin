package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class Password(val value: String, birthDate: LocalDate) {
    companion object {
        private const val MIN_LENGTH = 8
        private const val MAX_LENGTH = 16
    }

    init {
        val passwordRegex = Regex("^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]{$MIN_LENGTH,$MAX_LENGTH}$")
        if (!passwordRegex.matches(value)) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 $MIN_LENGTH ~ $MAX_LENGTH 자의 영문 대소문자, 숫자, 특수문자만 가능합니다.")
        }

        if (Regex("(.)\\1{2,}").containsMatchIn(value)) {
            throw CoreException(ErrorType.BAD_REQUEST, "동일 문자가 3회 이상 연속될 수 없습니다.")
        }

        val birthDateStr = birthDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        if (value.contains(birthDateStr)) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호에 생년월일을 포함할 수 없습니다.")
        }
    }
}
