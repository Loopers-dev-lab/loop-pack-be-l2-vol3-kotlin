package com.loopers.infrastructure.user

import com.loopers.domain.user.User
import org.springframework.stereotype.Component

@Component
class UserMapper {
    fun toDomain(entity: UserEntity): User {
        return User.retrieve(
            id = entity.id!!,
            loginId = entity.loginId,
            password = entity.password,
            name = entity.name,
            birthDate = entity.birthDate,
            email = entity.email,
        )
    }

    fun toEntity(user: User): UserEntity {
        return UserEntity(
            id = user.id,
            loginId = user.loginId.value,
            password = user.password.value,
            name = user.name.value,
            birthDate = user.birthDate,
            email = user.email.value,
        )
    }
}
