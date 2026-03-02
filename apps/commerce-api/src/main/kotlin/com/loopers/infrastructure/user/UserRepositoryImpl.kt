package com.loopers.infrastructure.user

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.user.model.User
import com.loopers.domain.user.repository.UserRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface UserJpaRepository : JpaRepository<UserEntity, Long> {
    fun findByLoginId(loginId: String): UserEntity?
    fun existsByLoginId(loginId: String): Boolean
}

@Repository
class UserRepositoryImpl(
    private val userJpaRepository: UserJpaRepository,
) : UserRepository {

    override fun save(user: User): User {
        return userJpaRepository.save(UserEntity.fromDomain(user)).toDomain()
    }

    override fun findById(id: UserId): User? {
        return userJpaRepository.findById(id.value).orElse(null)?.toDomain()
    }

    override fun findByLoginId(loginId: String): User? {
        return userJpaRepository.findByLoginId(loginId)?.toDomain()
    }

    override fun existsByLoginId(loginId: String): Boolean {
        return userJpaRepository.existsByLoginId(loginId)
    }
}
