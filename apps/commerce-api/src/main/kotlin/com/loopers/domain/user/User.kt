package com.loopers.domain.user

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "users")
class User(
    loginId: String,
    password: String,
    name: String,
    birthDate: LocalDate,
    email: String,
) : BaseEntity() {

    @Column(name = "login_id", nullable = false, unique = true)
    var loginId: String = loginId
        private set

    @Column(name = "password", nullable = false)
    var password: String = password
        private set

    @Column(name = "name", nullable = false)
    var name: String = name
        private set

    @Column(name = "birth_date", nullable = false)
    var birthDate: LocalDate = birthDate
        private set

    @Column(name = "email", nullable = false)
    var email: String = email
        private set

    init {
        validateLoginId(loginId)
        validateName(name)
        validateEmail(email)
    }

    private fun validateLoginId(loginId: String) {
        if (loginId.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 빈 값일 수 없습니다.")
        if (!loginId.matches(LOGIN_ID_PATTERN)) throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 영문과 숫자만 허용됩니다.")
        if (loginId.length > MAX_LOGIN_ID_LENGTH) {
            throw CoreException(
                ErrorType.BAD_REQUEST,
                "로그인 ID는 ${MAX_LOGIN_ID_LENGTH}자 이하여야 합니다.",
            )
        }
    }

    private fun validateName(name: String) {
        if (name.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "이름은 빈 값일 수 없습니다.")
    }

    private fun validateEmail(email: String) {
        if (!email.matches(EMAIL_PATTERN)) throw CoreException(ErrorType.BAD_REQUEST, "올바른 이메일 형식이 아닙니다.")
    }

    fun getMaskedName(): String = if (name.length <= 1) MASK_CHAR else name.dropLast(1) + MASK_CHAR

    fun changePassword(newEncodedPassword: String) {
        this.password = newEncodedPassword
    }

    companion object {
        private val LOGIN_ID_PATTERN = Regex("^[a-zA-Z0-9]+$")
        private val EMAIL_PATTERN = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        private const val MAX_LOGIN_ID_LENGTH = 10
        private const val MASK_CHAR = "*"
    }
}
