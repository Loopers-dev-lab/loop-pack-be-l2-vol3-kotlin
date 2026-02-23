package com.loopers.infrastructure.user

import com.loopers.domain.BaseEntity
import com.loopers.domain.user.entity.User
import com.loopers.domain.user.vo.Email
import com.loopers.domain.user.vo.LoginId
import com.loopers.domain.user.vo.Name
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
            ).also { entity ->
                if (user.id != 0L) {
                    setBaseEntityField(entity, "id", user.id)
                    setBaseEntityField(entity, "createdAt", user.createdAt)
                    setBaseEntityField(entity, "updatedAt", user.updatedAt)
                }
                user.deletedAt?.let { setBaseEntityField(entity, "deletedAt", it) }
            }
        }

        private fun setBaseEntityField(entity: BaseEntity, fieldName: String, value: Any) {
            BaseEntity::class.java.getDeclaredField(fieldName).apply {
                isAccessible = true
                set(entity, value)
            }
        }
    }

    fun toDomain(): User = User(
        id = id,
        loginId = LoginId(loginId),
        password = password,
        name = Name(name),
        birthDate = birthDate,
        email = Email(email),
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}
