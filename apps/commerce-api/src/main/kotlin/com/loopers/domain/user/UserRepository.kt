package com.loopers.domain.user

interface UserRepository {

    fun save(user: User): Long

    fun existsByLoginId(loginId: LoginId): Boolean
}
