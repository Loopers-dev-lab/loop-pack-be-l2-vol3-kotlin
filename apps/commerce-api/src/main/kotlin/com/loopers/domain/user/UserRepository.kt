package com.loopers.domain.user

interface UserRepository {
    fun find(id: Long): User?
    fun findByLoginId(loginId: LoginId): User?
    fun existsByLoginId(loginId: LoginId): Boolean
    fun save(user: User): User
}
