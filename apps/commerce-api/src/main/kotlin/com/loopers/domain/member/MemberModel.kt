package com.loopers.domain.member

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "member")
class MemberModel(
    loginId: String,
    password: String,
    name: String,
    birthDate: LocalDate,
    email: String,
) : BaseEntity() {

    @Column(name = "login_id", nullable = false, unique = true, length = 20)
    var loginId: String = loginId
        protected set

    @Column(name = "password", nullable = false)
    var password: String = password
        protected set

    @Column(name = "name", nullable = false, length = 50)
    var name: String = name
        protected set

    @Column(name = "birth_date", nullable = false)
    var birthDate: LocalDate = birthDate
        protected set

    @Column(name = "email", nullable = false)
    var email: String = email
        protected set

    init {
        validateLoginId(loginId)
        validateName(name)
        validateEmail(email)
        validateBirthDate(birthDate)
    }

    private fun validateLoginId(loginId: String) {
        if (loginId.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 비어있을 수 없습니다.")
        }
        if (loginId.length < 4 || loginId.length > 20) {
            throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 4~20자여야 합니다.")
        }
        if (!loginId.matches(Regex("^[a-z0-9_]+$"))) {
            throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 영문 소문자, 숫자, 언더스코어만 허용됩니다.")
        }
    }

    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "이름은 비어있을 수 없습니다.")
        }
        if (name.length < 2 || name.length > 50) {
            throw CoreException(ErrorType.BAD_REQUEST, "이름은 2~50자여야 합니다.")
        }
        if (!name.matches(Regex("^[가-힣a-zA-Z\\s]+$"))) {
            throw CoreException(ErrorType.BAD_REQUEST, "이름은 한글 또는 영문만 허용됩니다.")
        }
    }

    private fun validateEmail(email: String) {
        if (email.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "이메일은 비어있을 수 없습니다.")
        }
        val emailRegex = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        if (!email.matches(emailRegex)) {
            throw CoreException(ErrorType.BAD_REQUEST, "이메일 형식이 올바르지 않습니다.")
        }
    }

    fun changePassword(newEncodedPassword: String) {
        this.password = newEncodedPassword
    }

    private fun validateBirthDate(birthDate: LocalDate) {
        if (birthDate.isAfter(LocalDate.now())) {
            throw CoreException(ErrorType.BAD_REQUEST, "생년월일은 미래 날짜일 수 없습니다.")
        }
    }
}
