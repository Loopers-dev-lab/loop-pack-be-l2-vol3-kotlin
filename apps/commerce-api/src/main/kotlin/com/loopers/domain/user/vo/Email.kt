package com.loopers.domain.user.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class Email(
    @Column(name = "email", nullable = false, length = 255)
    val value: String,
) {

    fun validate() {
        validateNotBlank()
        validateEmailFormat()
    }

    private fun validateNotBlank() {
        if (value.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "이메일은 비어있을 수 없습니다.")
        }
    }

    private fun validateEmailFormat() {
        val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        if (!value.matches(emailPattern)) {
            throw CoreException(ErrorType.BAD_REQUEST, "유효하지 않은 이메일 형식입니다.")
        }
    }

    companion object {
        fun of(email: String): Email {
            val e = Email(email)
            e.validate()
            return e
        }
    }
}
