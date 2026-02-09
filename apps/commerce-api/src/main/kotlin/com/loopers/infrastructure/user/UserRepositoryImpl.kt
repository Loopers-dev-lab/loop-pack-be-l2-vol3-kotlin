package com.loopers.infrastructure.user

import com.loopers.domain.user.User
import com.loopers.domain.user.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class UserRepositoryImpl(
    private val userJpaRepository: UserJpaRepository,
) : UserRepository {

    override fun save(user: User): User {
        return userJpaRepository.save(user)
    }

    override fun findByLoginId(loginId: String): User? {
        return userJpaRepository.findByLoginId(loginId)
    }

    override fun findById(id: Long): User? {
        return userJpaRepository.findByIdOrNull(id)
    }
}
