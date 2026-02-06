package com.loopers.domain.user

interface UserRepository {
    fun save(user: UserModel): UserModel
    fun findByLoginId(loginId: LoginId): UserModel?
    fun existsByLoginId(loginId: LoginId): Boolean
}
