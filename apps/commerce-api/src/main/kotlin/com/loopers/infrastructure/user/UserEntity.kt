package com.loopers.infrastructure.user

import com.loopers.domain.BaseEntity
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.user.model.User
import com.loopers.domain.user.vo.Email
import com.loopers.domain.user.vo.LoginId
import com.loopers.domain.user.vo.Name
import com.loopers.domain.withBaseFields
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "users")
class UserEntity(
    @Column(name = "login_id", nullable = false, unique = true, length = 16)
    var loginId: String,

    @Column(name = "password", nullable = false)
    var password: String,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "birth_date", nullable = false)
    var birthDate: LocalDate,

    @Column(name = "email", nullable = false)
    var email: String,
) : BaseEntity() {

    companion object {
        fun fromDomain(user: User): UserEntity {
            return UserEntity(
                loginId = user.loginId.value,
                password = user.password,
                name = user.name.value,
                birthDate = user.birthDate,
                email = user.email.value,
            ).withBaseFields(
                id = user.id.value,
                deletedAt = user.deletedAt,
            )
        }
    }

    fun toDomain(): User = User(
        id = UserId(id),
        loginId = LoginId(loginId),
        password = password,
        name = Name(name),
        birthDate = birthDate,
        email = Email(email),
        deletedAt = deletedAt,
    )
}
