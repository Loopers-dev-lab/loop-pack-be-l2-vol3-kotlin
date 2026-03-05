package com.loopers.infrastructure.user

import com.loopers.domain.user.User
import com.loopers.domain.user.UserRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryImpl(
    private val userJpaRepository: UserJpaRepository,
    private val userMapper: UserMapper,
) : UserRepository {
    override fun save(user: User): User {
        try {
            return userMapper.toDomain(userJpaRepository.saveAndFlush(userMapper.toEntity(user)))
        } catch (e: DataIntegrityViolationException) {
            throw CoreException(ErrorType.USER_DUPLICATE_LOGIN_ID)
        }
    }

    override fun existsByLoginId(loginId: String): Boolean {
        return userJpaRepository.existsByLoginId(loginId)
    }

    override fun findByLoginId(loginId: String): User? {
        return userJpaRepository.findByLoginId(loginId)?.let { userMapper.toDomain(it) }
    }
}
