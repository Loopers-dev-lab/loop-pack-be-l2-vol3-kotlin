package com.loopers.domain.user

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "users")
class User(
    loginId: String,
    password: String,
    name: String,
    birthday: LocalDate,
    email: String,
) : BaseEntity() {

    var loginId: String = loginId
        protected set

    var password: String = password
        protected set

    var name: String = name
        protected set

    var birthday: LocalDate = birthday
        protected set

    var email: String = email
        protected set

    init {
        LoginId.of(loginId)
        validateName(name)
        Email.of(email)
    }

    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "이름은 비어있을 수 없습니다.")
        }
    }

    fun changePassword(currentPassword: String, newPassword: String, passwordEncoder: PasswordEncoder) {
        if (!passwordEncoder.matches(currentPassword, this.password)) {
            throw CoreException(ErrorType.UNAUTHORIZED, "현재 비밀번호가 일치하지 않습니다.")
        }
        if (currentPassword == newPassword) {
            throw CoreException(ErrorType.BAD_REQUEST, "새 비밀번호는 현재 비밀번호와 달라야 합니다.")
        }
        val validatedPassword = Password.of(newPassword, this.birthday)
        this.password = passwordEncoder.encode(validatedPassword.value)
    }
}
