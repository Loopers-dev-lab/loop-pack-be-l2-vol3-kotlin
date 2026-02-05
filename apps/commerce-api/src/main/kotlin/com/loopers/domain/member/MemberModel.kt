package com.loopers.domain.member

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Entity
@Table(name = "member")
class MemberModel(
    loginId: String,
    password: String,
    name: String,
    birthDate: LocalDate,
    email: String,
    skipPasswordValidation: Boolean = false,
) : BaseEntity() {
    var loginId: String = loginId
        protected set

    var password: String = password
        protected set

    var name: String = name
        protected set

    var birthDate: LocalDate = birthDate
        protected set

    var email: String = email
        protected set

    init {
        validateLoginId(loginId)
        if (!skipPasswordValidation) {
            validatePassword(password, birthDate)
        }
        validateName(name)
        validateEmail(email)
    }

    fun changePassword(newPassword: String) {
        if (this.password == newPassword) {
            throw CoreException(ErrorType.BAD_REQUEST, "새 비밀번호는 현재 비밀번호와 달라야 합니다.")
        }
        validatePassword(newPassword, this.birthDate)
        this.password = newPassword
    }

    fun updateEncodedPassword(encodedPassword: String) {
        this.password = encodedPassword
    }

    private fun validateLoginId(loginId: String) {
        if (loginId.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "로그인ID는 비어있을 수 없습니다.")
        }
        if (!loginId.matches(Regex("^[a-zA-Z0-9]+$"))) {
            throw CoreException(ErrorType.BAD_REQUEST, "로그인ID는 영문과 숫자만 포함할 수 있습니다.")
        }
    }

    private fun validatePassword(password: String, birthDate: LocalDate) {
        if (password.length !in 8..16) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 8자 이상 16자 이하여야 합니다.")
        }
        if (!password.matches(Regex("^[a-zA-Z0-9!@#\$%^&*()_+\\-=\\[\\]{}|;':\",./<>?]+$"))) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 영문 대소문자, 숫자, 특수문자만 포함할 수 있습니다.")
        }

        val birthDateYyyyMMdd = birthDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val birthDateYyyyDashMMDashDd = birthDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        if (password.contains(birthDateYyyyMMdd) || password.contains(birthDateYyyyDashMMDashDd)) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호에 생년월일이 포함될 수 없습니다.")
        }
    }

    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "이름은 비어있을 수 없습니다.")
        }
    }

    private fun validateEmail(email: String) {
        if (email.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "이메일은 비어있을 수 없습니다.")
        }
        if (!email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
            throw CoreException(ErrorType.BAD_REQUEST, "올바른 이메일 형식이 아닙니다.")
        }
    }
}
