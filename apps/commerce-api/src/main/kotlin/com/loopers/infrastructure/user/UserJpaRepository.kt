package com.loopers.infrastructure.user

import com.loopers.domain.user.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserJpaRepository : JpaRepository<User, Long> {
    fun existsByLoginId(loginId: String): Boolean
    fun findByLoginId(loginId: String): User?
}
