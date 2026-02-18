package com.loopers.domain.user.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

@JvmInline
value class Name(val value: String) {

    fun validate() {
        validateNotBlank()
        validateLength()
    }

    private fun validateNotBlank() {
        if (value.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "이름은 비어있을 수 없습니다.")
        }
    }

    private fun validateLength() {
        if (value.length < 2) {
            throw CoreException(ErrorType.BAD_REQUEST, "이름은 2자 이상이어야 합니다.")
        }
    }

    companion object {
        fun of(name: String): Name {
            val n = Name(name)
            n.validate()
            return n
        }
    }
}
