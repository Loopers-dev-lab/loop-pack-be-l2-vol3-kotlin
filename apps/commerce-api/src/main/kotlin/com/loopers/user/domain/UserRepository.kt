package com.loopers.user.domain

interface UserRepository {
    fun save(user: User): User

    fun existsByLoginId(loginId: String): Boolean
}
