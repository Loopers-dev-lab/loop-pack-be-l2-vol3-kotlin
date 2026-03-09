package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.time.ZonedDateTime

class Password private constructor(
    val value: String,
) {
    companion object {
        private val PASSWORD_REGEX =
            Regex("^[A-Za-z0-9!@#\$%^&*()_+\\-={}\\[\\]:;\"'<>,.?/]{8,16}$")

        fun of(rawPassword: String, birthDate: ZonedDateTime): Password {
            val birth = birthDate.toLocalDate().toString().replace("-", "")

            if (rawPassword.isBlank()) {
                throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 비어있을 수 없습니다.")
            }
            if (rawPassword.length !in 8..16) {
                throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 8~16자여야 합니다.")
            }
            if (!PASSWORD_REGEX.matches(rawPassword)) {
                throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 영문 대소문자, 숫자, 특수문자만 사용할 수 있습니다.")
            }
            if (rawPassword.contains(birth)) {
                throw CoreException(ErrorType.BAD_REQUEST, "비밀번호에 생년월일을 포함할 수 없습니다.")
            }
            return Password(rawPassword)
        }
    }
}
