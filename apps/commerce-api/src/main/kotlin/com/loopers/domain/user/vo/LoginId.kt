package com.loopers.domain.user.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

@JvmInline
value class LoginId(val value: String) {

    fun validate() {
        validateNotBlank()
        validateLength()
        validateCharacters()
    }

    private fun validateNotBlank() {
        if (value.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "로그인 아이디는 비어있을 수 없습니다.")
        }
    }

    private fun validateLength() {
        if (value.length < 4) {
            throw CoreException(ErrorType.BAD_REQUEST, "로그인 아이디는 4자 이상이어야 합니다.")
        }
        if (value.length > 20) {
            throw CoreException(ErrorType.BAD_REQUEST, "로그인 아이디는 20자 이하여야 합니다.")
        }
    }

    private fun validateCharacters() {
        val validCharacterPattern = Regex("^[a-zA-Z0-9]+\$")
        if (!value.matches(validCharacterPattern)) {
            throw CoreException(ErrorType.BAD_REQUEST, "로그인 아이디는 영문과 숫자만 포함할 수 있습니다.")
        }
    }

    companion object {
        fun of(loginId: String): LoginId {
            val id = LoginId(loginId)
            id.validate()
            return id
        }
    }
}
