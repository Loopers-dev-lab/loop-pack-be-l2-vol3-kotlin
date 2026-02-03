package com.loopers.domain.user

interface UserRepository {
    fun save(user: User): Long

    fun findByLoginId(loginId: LoginId): User?

    fun existsByLoginId(loginId: LoginId): Boolean
}
