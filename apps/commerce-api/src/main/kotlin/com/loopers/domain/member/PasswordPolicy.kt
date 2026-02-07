package com.loopers.domain.member

import com.loopers.domain.member.vo.Password
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class PasswordPolicy(private val encoder: PasswordEncoder) {

    fun createPassword(rawPassword: String, birthDate: LocalDate): Password {
        validateFormat(rawPassword)
        validateNotContainsBirthDate(rawPassword, birthDate)
        return Password.fromEncoded(encoder.encode(rawPassword))
    }

    fun matches(rawPassword: String, password: Password): Boolean {
        return password.matches(rawPassword, encoder)
    }

    private fun validateFormat(rawPassword: String) {
        if (rawPassword.length < MIN_LENGTH || rawPassword.length > MAX_LENGTH) {
            throw CoreException(ErrorType.INVALID_PASSWORD_FORMAT, "비밀번호는 ${MIN_LENGTH}~${MAX_LENGTH}자여야 합니다.")
        }
        if (!rawPassword.matches(ALLOWED_CHARS_PATTERN)) {
            throw CoreException(ErrorType.INVALID_PASSWORD_FORMAT, "비밀번호는 영문 대소문자, 숫자, 특수문자만 사용 가능합니다.")
        }
    }

    private fun validateNotContainsBirthDate(rawPassword: String, birthDate: LocalDate) {
        val birthDateStrings = BIRTH_DATE_FORMATS.map { birthDate.format(it) }
        if (birthDateStrings.any { rawPassword.contains(it) }) {
            throw CoreException(ErrorType.PASSWORD_CONTAINS_BIRTHDATE)
        }
    }

    companion object {
        private val ALLOWED_CHARS_PATTERN = Regex("^[a-zA-Z0-9!@#\$%^&*()_+\\-=\\[\\]{}|;':\",./<>?`~]+$")
        private const val MIN_LENGTH = 8
        private const val MAX_LENGTH = 16

        private val BIRTH_DATE_FORMATS = listOf(
            DateTimeFormatter.ofPattern("yyyyMMdd"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        )
    }
}
