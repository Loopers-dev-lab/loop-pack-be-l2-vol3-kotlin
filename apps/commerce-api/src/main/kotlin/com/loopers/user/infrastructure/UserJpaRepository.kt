package com.loopers.user.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface UserJpaRepository : JpaRepository<UserEntity, Long> {
    fun existsByLoginId(loginId: String): Boolean
}
