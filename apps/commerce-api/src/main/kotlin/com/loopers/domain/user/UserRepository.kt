package com.loopers.domain.user

interface UserRepository {
    fun save(user: User): User

    fun existsByLoginId(loginId: String): Boolean

    fun findByLoginId(loginId: String): User?
}
