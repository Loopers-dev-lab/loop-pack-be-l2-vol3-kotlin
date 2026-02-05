package com.loopers.infrastructure.user

import com.loopers.domain.user.BirthDate
import com.loopers.domain.user.Email
import com.loopers.domain.user.LoginId
import com.loopers.domain.user.Name
import com.loopers.domain.user.Password
import com.loopers.domain.user.User

object UserMapper {

    fun toDomain(entity: UserEntity): User {
        val id = requireNotNull(entity.id) {
            "UserEntity.id가 null입니다. 저장된 Entity만 Domain으로 변환 가능합니다."
        }
        return User.reconstitute(
            persistenceId = id,
            loginId = LoginId(entity.loginId),
            password = Password.fromEncoded(entity.password),
            name = Name(entity.name),
            birthDate = BirthDate(entity.birthDate),
            email = Email(entity.email),
            gender = entity.gender,
        )
    }

    fun toEntity(domain: User): UserEntity {
        return UserEntity(
            id = domain.persistenceId,
            loginId = domain.loginId.value,
            password = domain.password.toEncodedString(),
            name = domain.name.value,
            birthDate = domain.birthDate.value,
            email = domain.email.value,
            gender = domain.gender,
        )
    }
}
