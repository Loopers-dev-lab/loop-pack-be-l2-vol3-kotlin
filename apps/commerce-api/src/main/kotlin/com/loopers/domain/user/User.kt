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
    loginId: LoginId,
    password: String,
    name: String,
    birthday: LocalDate,
    email: Email,
) : BaseEntity() {

    var loginId: LoginId = loginId
        protected set

    var password: String = password
        protected set

    var name: String = name
        protected set

    var birthday: LocalDate = birthday
        protected set

    var email: Email = email
        protected set

    init {
        validateName(name)
    }

    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "이름은 비어있을 수 없습니다.")
        }
    }

    fun changePassword(encodedNewPassword: String) {
        this.password = encodedNewPassword
    }
}
