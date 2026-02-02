package com.loopers.domain.user

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "users")
class UserModel(
    username: String,
    password: String,
    name: String,
    email: String,
    birthDate: ZonedDateTime
) : BaseEntity() {
    val username: String = username

    var password: String = password
        protected set

    val name: String = name

    val email: String = email

    val birthDate: ZonedDateTime = birthDate

    init {
        validateCreationInvariants()
        validatePassword(password)
    }

    fun updatePassword(newPassword: String) {
        if (password == newPassword) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 이전과 동일할 수 없습니다.")
        }
        validatePassword(newPassword)
        this.password = newPassword
    }

    private fun validateCreationInvariants() {
        if (username.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "아이디는 비어있을 수 없습니다.")
        }
        if (!USERNAME_REGEX.matches(username)) {
            throw CoreException(ErrorType.BAD_REQUEST, "아이디는 영문과 숫자만 사용할 수 있습니다.")
        }
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "이름은 비어있을 수 없습니다.")
        }
        if (email.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "이메일은 비어있을 수 없습니다.")
        }
        if (!EMAIL_REGEX.matches(email)) {
            throw CoreException(ErrorType.BAD_REQUEST, "이메일 형식이 올바르지 않습니다.")
        }
        if (birthDate.isAfter(ZonedDateTime.now())) {
            throw CoreException(ErrorType.BAD_REQUEST, "생년월일은 현재 시점 이후일 수 없습니다.")
        }
    }

    private fun validatePassword(password: String) {
        if (password.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 비어있을 수 없습니다.")
        }
        if (password.length !in 8..16) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 8~16자여야 합니다.")
        }
        if (!PASSWORD_REGEX.matches(password)) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 영문 대소문자, 숫자, 특수문자만 사용할 수 있습니다.")
        }
        val birth = birthDate.toLocalDate().toString().replace("-", "")
        if (password.contains(birth)) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호에 생년월일을 포함할 수 없습니다.")
        }
    }

    companion object {
        private val USERNAME_REGEX =
            Regex("^[a-zA-Z0-9]+$")
        private val PASSWORD_REGEX =
            Regex("^[A-Za-z0-9!@#\$%^&*()_+\\-={}\\[\\]:;\"'<>,.?/]{8,16}$")
        private val EMAIL_REGEX =
            Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
