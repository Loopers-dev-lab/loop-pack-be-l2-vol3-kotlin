package com.loopers.domain.member.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class Password private constructor(val value: String) {

    fun matches(rawPassword: String): Boolean {
        return ENCODER.matches(rawPassword, value)
    }

    override fun toString(): String = "Password(****)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Password) return false
        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()

    companion object {
        private val ENCODER = BCryptPasswordEncoder()
        private val ALLOWED_CHARS_PATTERN = Regex("^[a-zA-Z0-9!@#\$%^&*()_+\\-=\\[\\]{}|;':\",./<>?`~]+$")
        private const val MIN_LENGTH = 8
        private const val MAX_LENGTH = 16

        private val BIRTH_DATE_FORMATS = listOf(
            DateTimeFormatter.ofPattern("yyyyMMdd"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        )

        fun of(rawPassword: String, birthDate: LocalDate): Password {
            validateFormat(rawPassword)
            validateNotContainsBirthDate(rawPassword, birthDate)
            return Password(ENCODER.encode(rawPassword))
        }

        fun fromEncoded(encodedPassword: String): Password {
            return Password(encodedPassword)
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
    }
}
