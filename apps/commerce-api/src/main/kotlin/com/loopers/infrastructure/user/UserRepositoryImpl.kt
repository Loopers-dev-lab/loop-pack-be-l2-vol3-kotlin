package com.loopers.infrastructure.user

import com.loopers.domain.user.User
import com.loopers.domain.user.UserRepository
import com.loopers.domain.user.vo.LoginId
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class UserRepositoryImpl(
    private val userJpaRepository: UserJpaRepository,
) : UserRepository {
    override fun existsByLoginId(loginId: LoginId) = userJpaRepository.existsByLoginId(loginId)
    override fun save(user: User): User = userJpaRepository.save(user)
    override fun findUserById(id: Long): User? = userJpaRepository.findByIdOrNull(id)
    override fun findByLoginId(loginId: LoginId): User? = userJpaRepository.findByLoginId(loginId)
}
