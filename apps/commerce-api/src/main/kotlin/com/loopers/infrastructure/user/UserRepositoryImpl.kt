package com.loopers.infrastructure.user

import com.loopers.domain.user.LoginId
import com.loopers.domain.user.UserModel
import com.loopers.domain.user.UserRepository
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryImpl(
    private val userJpaRepository: UserJpaRepository,
) : UserRepository {

    override fun save(user: UserModel): UserModel {
        return userJpaRepository.save(user)
    }

    override fun findByLoginId(loginId: LoginId): UserModel? {
        return userJpaRepository.findByLoginId(loginId)
    }

    override fun existsByLoginId(loginId: LoginId): Boolean {
        return userJpaRepository.existsByLoginId(loginId)
    }
}
