package com.loopers.infrastructure.user

import org.springframework.data.jpa.repository.JpaRepository

interface UserJpaRepository : JpaRepository<UserEntity, Long> {
    fun existsByLoginId(loginId: String): Boolean

    fun findByLoginId(loginId: String): UserEntity?
}
