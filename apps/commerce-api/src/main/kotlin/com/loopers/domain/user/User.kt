package com.loopers.domain.user

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Entity
@Table(name = "user")
class User(
    loginId: String,
    password: String,
    name: String,
    birthDate: LocalDate,
    email: String,
) : BaseEntity() {

    @Column(name = "login_id", nullable = false, unique = true, length = 10)
    var loginId: String = loginId
        protected set

    @Column(name = "password", nullable = false)
    var password: String = password
        protected set

    @Column(name = "name", nullable = false)
    var name: String = name
        protected set

    @Column(name = "birth_date", nullable = false)
    var birthDate: LocalDate = birthDate
        protected set

    @Column(name = "email", nullable = false)
    var email: String = email
        protected set

    init {
        validateEmail(email)
        validateLoginId(loginId)
        validatePassword(password, birthDate)
        this.password = encodePassword(password)
    }

    fun getMaskedName(): String {
        return ""
    }

    fun changePassword(newPassword: String) {
    }

    fun verifyPassword(rawPassword: String): Boolean {
        return true
    }

    private fun encodePassword(rawPassword: String): String {
        val bytes = rawPassword.toByteArray()
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    private fun validatePassword(password: String, birthDate: LocalDate) {
        val passwordRegex = Regex("^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]{8,16}$")
        if (!passwordRegex.matches(password)) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 8~16자의 영문 대소문자, 숫자, 특수문자만 가능합니다.")
        }

        val birthDateStr = birthDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        if (password.contains(birthDateStr)) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호에 생년월일을 포함할 수 없습니다.")
        }
    }

    private fun validateLoginId(loginId: String) {
        if (!Regex("^[a-zA-Z0-9]+$").matches(loginId)) {
            throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 영문 및 숫자만 허용됩니다.")
        }
    }

    private fun validateEmail(email: String) {
        if (!Regex("^[^@]+@[^@]+\\.[^@]+$").matches(email)) {
            throw CoreException(ErrorType.BAD_REQUEST, "이메일 형식이 올바르지 않습니다.")
        }
    }
}
