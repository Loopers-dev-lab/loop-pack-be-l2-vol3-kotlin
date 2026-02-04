package com.loopers.infrastructure.user

import com.loopers.domain.user.BirthDate
import com.loopers.domain.user.Email
import com.loopers.domain.user.LoginId
import com.loopers.domain.user.Name
import com.loopers.domain.user.Password
import com.loopers.domain.user.User

object UserMapper {

    /**
     * Converts a persisted UserEntity into a domain User by reconstituting domain value objects from the entity's fields.
     *
     * @param entity The persisted user entity to convert.
     * @return A domain User reconstructed from the entity's data.
     */
    fun toDomain(entity: UserEntity): User {
        return User.reconstitute(
            persistenceId = entity.id!!,
            loginId = LoginId(entity.loginId),
            password = Password.fromEncoded(entity.password),
            name = Name(entity.name),
            birthDate = BirthDate(entity.birthDate),
            email = Email(entity.email),
            gender = entity.gender,
        )
    }

    /**
     * Converts a domain User into a persistence UserEntity.
     *
     * @param domain The domain User to convert.
     * @return A UserEntity populated with the domain user's persistenceId, loginId, encoded password, name, birthDate, email, and gender.
     */
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