package com.loopers.domain.user

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Entity
@Table(name = "users")
class User private constructor(
    loginId: String,
    password: String,
    name: String,
    birthDate: String,
    email: String,
) : BaseEntity() {

    var loginId: String = loginId
        protected set

    var password: String = password
        protected set

    var name: String = name
        protected set

    var birthDate: String = birthDate
        protected set

    var email: String = email
        protected set

    init {
        validateLoginId(loginId)
        validatePassword(password)
        validateName(name)
        validateBirthDate(birthDate)
        validateEmail(email)
    }

    companion object {
        fun create(
            loginId: String,
            password: String,
            name: String,
            birthDate: String,
            email: String,
        ): User {
            return User(
                loginId = loginId,
                password = password,
                name = name,
                birthDate = birthDate,
                email = email,
            )
        }
    }

    private fun validateLoginId(loginId: String) {
        if (loginId.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 비어있을 수 없습니다.")
        if (loginId.length !in 4..20) throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 4자 이상 20자 이하여야 합니다.")
        if (!loginId.matches(Regex("^[a-zA-Z0-9]+$"))) throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 영문과 숫자로만 이루어져야 합니다.")
    }

    private fun validatePassword(password: String) {
        if (password.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 비어있을 수 없습니다.")
    }

    private fun validateName(name: String) {
        if (name.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "이름은 비어있을 수 없습니다.")
        if (name.length <= 1) throw CoreException(ErrorType.BAD_REQUEST, "이름은 2글자 이상이어야 합니다.")
    }

    private fun validateBirthDate(birthDate: String) {
        if (birthDate.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "생년월일은 비어있을 수 없습니다.")
        if (!isValidDate(birthDate)) throw CoreException(ErrorType.BAD_REQUEST, "생년월일은 날짜형식이어야 합니다.")
    }

    private fun validateEmail(email: String) {
        if (email.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "이메일은 비어있을 수 없습니다.")
        if (!email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"))) {
            throw CoreException(
            ErrorType.BAD_REQUEST,
            "이메일은 이메일 형식에 맞아야 합니다.",
        )
        }
    }

    private fun isValidDate(dateStr: String, pattern: String = "yyyyMMdd"): Boolean {
        return runCatching {
            LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern))
        }.isSuccess
    }

    fun changePassword(newPassword: String) {
        password = newPassword
    }
}
