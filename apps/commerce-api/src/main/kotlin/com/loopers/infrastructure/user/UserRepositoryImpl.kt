package com.loopers.infrastructure.user

import com.loopers.domain.user.model.User
import com.loopers.domain.user.repository.UserRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface UserJpaRepository : JpaRepository<UserEntity, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): UserEntity?
    fun findByLoginIdAndDeletedAtIsNull(loginId: String): UserEntity?
    fun existsByLoginIdAndDeletedAtIsNull(loginId: String): Boolean
}

@Repository
class UserRepositoryImpl(
    private val userJpaRepository: UserJpaRepository,
) : UserRepository {

    override fun save(user: User): User {
        return userJpaRepository.save(UserEntity.fromDomain(user)).toDomain()
    }

    override fun findById(id: Long): User? {
        return userJpaRepository.findByIdAndDeletedAtIsNull(id)?.toDomain()
    }

    override fun findByLoginId(loginId: String): User? {
        return userJpaRepository.findByLoginIdAndDeletedAtIsNull(loginId)?.toDomain()
    }

    override fun existsByLoginId(loginId: String): Boolean {
        return userJpaRepository.existsByLoginIdAndDeletedAtIsNull(loginId)
    }
}
