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
        return requireNotNull(saved.id) {
            "User 저장 실패: id가 생성되지 않았습니다."
        }
    }

    override fun findById(id: Long): User? {
        return jpaRepository.findById(id).orElse(null)?.let { UserMapper.toDomain(it) }
    }

    override fun findByLoginId(loginId: LoginId): User? {
        return jpaRepository.findByLoginId(loginId.value)?.let { UserMapper.toDomain(it) }
    }

    override fun existsByLoginId(loginId: LoginId): Boolean {
        return jpaRepository.existsByLoginId(loginId.value)
    }
}
