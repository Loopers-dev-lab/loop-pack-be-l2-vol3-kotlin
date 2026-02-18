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
    username: Username,
    password: Password,
    name: String,
    email: Email,
    birthDate: ZonedDateTime,
) : BaseEntity() {
    val username: String = username.value

    var password: String = password.value
        protected set

    val name: String = name

    val email: String = email.value

    val birthDate: ZonedDateTime = birthDate

    init {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "이름은 비어있을 수 없습니다.")
        }
        if (birthDate.isAfter(ZonedDateTime.now())) {
            throw CoreException(ErrorType.BAD_REQUEST, "생년월일은 현재 시점 이후일 수 없습니다.")
        }
    }

    fun applyEncodedPassword(encodedPassword: String) {
        this.password = encodedPassword
    }
}
