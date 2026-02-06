package com.loopers.user.infrastructure

import com.loopers.domain.BaseEntity
import com.loopers.user.domain.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDate

@Table(name = "users")
@Entity
class UserEntity(
    @Column(nullable = false, unique = true)
    val loginId: String,
    @Column(nullable = false)
    val password: String,
    @Column(nullable = false)
    val name: String,
    @Column(nullable = false)
    val birthDate: LocalDate,
    @Column(nullable = false)
    val email: String,
) : BaseEntity() {
    fun toDomain(): User {
        return User.retrieve(
            loginId = loginId,
            password = password,
            name = name,
            birthDate = birthDate,
            email = email,
        )
    }

    companion object {
        fun from(user: User): UserEntity {
            return UserEntity(
                loginId = user.loginId,
                password = user.password,
                name = user.name,
                birthDate = user.birthDate,
                email = user.email,
            )
        }
    }
}
