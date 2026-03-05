package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class RawPassword(val value: String) {
    init {
        if (!PATTERN.matches(value)) {
            throw CoreException(ErrorType.USER_INVALID_PASSWORD)
        }
    }

    companion object {
        private val PATTERN = Regex("^[a-zA-Z0-9!@#\$%^&*()_+\\-=\\[\\]{}|;:',.<>?/]+$")
        private val BIRTH_DATE_COMPACT = DateTimeFormatter.ofPattern("yyyyMMdd")

        fun withBirthDateValidation(value: String, birthDate: LocalDate): RawPassword {
            val compactDate = birthDate.format(BIRTH_DATE_COMPACT)
            val dashedDate = birthDate.toString()
            if (value.contains(compactDate) || value.contains(dashedDate)) {
                throw CoreException(ErrorType.USER_INVALID_PASSWORD, "비밀번호에 생년월일을 포함할 수 없습니다.")
            }
            return RawPassword(value)
        }
    }
}
