package com.loopers.infrastructure.user

import com.loopers.domain.user.UserModel
import com.loopers.domain.user.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class UserRepositoryImpl(
    private val userJpaRepository: UserJpaRepository,
) : UserRepository {
    override fun find(id: Long): UserModel? {
        return userJpaRepository.findByIdOrNull(id)
    }

    override fun findByUsername(username: String): UserModel? {
        return userJpaRepository.findByUsername(username)
    }

    override fun save(user: UserModel): UserModel {
        return userJpaRepository.save(user)
    }
}
