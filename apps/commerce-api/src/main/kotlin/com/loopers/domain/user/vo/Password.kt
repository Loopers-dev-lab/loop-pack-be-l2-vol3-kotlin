package com.loopers.domain.user.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

@JvmInline
value class Password(val value: String) {

    companion object {
        fun validate(plainPassword: String, birthDate: String) {
            validateNotBlank(plainPassword)
            validateLength(plainPassword)
            validateCharacters(plainPassword)
            validateNotContainsBirthDate(plainPassword, birthDate)
        }

        fun ofEncrypted(encryptedPassword: String): Password {
            return Password(encryptedPassword)
        }

        private fun validateNotBlank(password: String) {
            if (password.isBlank()) {
                throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 비어있을 수 없습니다.")
            }
        }

        private fun validateLength(password: String) {
            if (password.length < 8) {
                throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 8자 이상이어야 합니다.")
            }
            if (password.length > 16) {
                throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 16자 이하여야 합니다.")
            }
        }

        private fun validateCharacters(password: String) {
            val validCharacterPattern = Regex(
                "^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};'\",./<>?/\\\\|`~]+\$",
            )
            if (!password.matches(validCharacterPattern)) {
                val message = "비밀번호는 영문, 숫자, 특수문자만 포함할 수 있습니다."
                throw CoreException(ErrorType.BAD_REQUEST, message)
            }
        }

        private fun validateNotContainsBirthDate(password: String, birthDate: String) {
            if (password.contains(birthDate)) {
                throw CoreException(ErrorType.BAD_REQUEST, "비밀번호에 생일이 포함될 수 없습니다.")
            }
        }
    }
}
