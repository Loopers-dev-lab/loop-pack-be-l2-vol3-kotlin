package com.loopers.user.infrastructure

import com.loopers.user.domain.User
import com.loopers.user.domain.UserRepository
import org.springframework.stereotype.Component

@Component
class UserRepositoryImpl(
    private val userJpaRepository: UserJpaRepository,
) : UserRepository {
    override fun save(user: User): User {
        return userJpaRepository.save(UserEntity.from(user)).toDomain()
    }

    override fun existsByLoginId(loginId: String): Boolean {
        return userJpaRepository.existsByLoginId(loginId)
    }

    override fun findByLoginId(loginId: String): User? {
        return userJpaRepository.findByLoginId(loginId)?.toDomain()
    }
}
