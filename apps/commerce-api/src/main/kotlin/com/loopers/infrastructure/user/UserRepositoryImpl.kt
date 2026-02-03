package com.loopers.infrastructure.user

import com.loopers.domain.user.LoginId
import com.loopers.domain.user.User
import com.loopers.domain.user.UserRepository
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryImpl(
    private val jpaRepository: UserJpaRepository,
) : UserRepository {

    override fun save(user: User): Long {
        val entity = UserMapper.toEntity(user)
        val saved = jpaRepository.save(entity)
        return saved.id
    }

    override fun existsByLoginId(loginId: LoginId): Boolean {
        return jpaRepository.existsByLoginId(loginId.value)
    }
}
