package com.loopers.domain.user

import com.loopers.domain.BaseEntity
import com.loopers.domain.user.vo.BirthDate
import com.loopers.domain.user.vo.Email
import com.loopers.domain.user.vo.LoginId
import com.loopers.domain.user.vo.Name
import com.loopers.domain.user.vo.Password
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class User private constructor(
    loginId: LoginId,
    password: Password,
    name: Name,
    birthDate: BirthDate,
    email: Email,
) : BaseEntity() {

    @Embedded
    var loginId: LoginId = loginId
        protected set

    @Embedded
    var password: Password = password
        protected set

    @Embedded
    var name: Name = name
        protected set

    @Embedded
    var birthDate: BirthDate = birthDate
        protected set

    @Embedded
    var email: Email = email
        protected set

    companion object {
        fun create(
            loginId: LoginId,
            password: Password,
            name: Name,
            birthDate: BirthDate,
            email: Email,
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

    fun changePassword(newPassword: Password) {
        this.password = newPassword
    }
}
