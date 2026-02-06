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

    companion object {
        private const val PASSWORD_MIN_LENGTH = 8
        private const val PASSWORD_MAX_LENGTH = 16
        private const val LOGIN_ID_MAX_LENGTH = 16
        private const val NAME_MAX_LENGTH = 10
    }

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
        validateName(name)
        validateEmail(email)
        validateLoginId(loginId)
        validatePassword(password, birthDate)
        this.password = encodePassword(password)
    }

    fun getMaskedName(): String {
        if (name.isEmpty()) return ""
        if (name.length == 1) return "*"
        return name.dropLast(1) + "*"
    }

    fun changePassword(newPassword: String) {
        if (verifyPassword(newPassword)) {
            throw CoreException(ErrorType.BAD_REQUEST, "현재 비밀번호와 동일한 비밀번호는 사용할 수 없습니다.")
        }
        validatePassword(newPassword, birthDate)
        this.password = encodePassword(newPassword)
    }

    fun verifyPassword(rawPassword: String): Boolean {
        return this.password == encodePassword(rawPassword)
    }

    //TODO: 추후 security 추가 시 변경
    private fun encodePassword(rawPassword: String): String {
        val bytes = rawPassword.toByteArray()
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "이름은 필수입니다.")
        }
        if (name.length !in 1..NAME_MAX_LENGTH) {
            throw CoreException(ErrorType.BAD_REQUEST, "이름은 ${NAME_MAX_LENGTH}자 이내여야 합니다.")
        }
        if (!Regex("^[가-힣a-zA-Z]+$").matches(name)) {
            throw CoreException(ErrorType.BAD_REQUEST, "이름은 한글 또는 영문만 허용됩니다.")
        }
    }

    private fun validatePassword(password: String, birthDate: LocalDate) {
        val passwordRegex = Regex("^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]{$PASSWORD_MIN_LENGTH,$PASSWORD_MAX_LENGTH}$")
        if (!passwordRegex.matches(password)) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 $PASSWORD_MIN_LENGTH ~ $PASSWORD_MAX_LENGTH 자의 영문 대소문자, 숫자, 특수문자만 가능합니다.")
        }

        if (Regex("(.)\\1{2,}").containsMatchIn(password)) {
            throw CoreException(ErrorType.BAD_REQUEST, "동일 문자가 3회 이상 연속될 수 없습니다.")
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
        if (loginId.length !in 1..LOGIN_ID_MAX_LENGTH) {
            throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 ${LOGIN_ID_MAX_LENGTH}자를 초과할 수 없습니다.")
        }
    }

    private fun validateEmail(email: String) {
        if (!Regex("^[^@]+@[^@]+\\.[^@]+$").matches(email)) {
            throw CoreException(ErrorType.BAD_REQUEST, "이메일 형식이 올바르지 않습니다.")
        }
    }
}
