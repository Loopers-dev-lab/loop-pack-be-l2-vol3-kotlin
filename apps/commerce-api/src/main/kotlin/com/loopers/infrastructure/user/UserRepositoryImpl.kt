package com.loopers.infrastructure.user

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
    private val jpa: UserJpaRepository,
) : UserRepository {

    override fun save(user: User): User {
        return jpa.save(UserEntity.fromDomain(user)).toDomain()
    }

    override fun findById(id: Long): User? {
        return jpa.findById(id).orElse(null)?.toDomain()
    }

    override fun findByLoginId(loginId: String): User? {
        return jpa.findByLoginId(loginId)?.toDomain()
    }

    override fun existsByLoginId(loginId: String): Boolean {
        return jpa.existsByLoginId(loginId)
    }
}
