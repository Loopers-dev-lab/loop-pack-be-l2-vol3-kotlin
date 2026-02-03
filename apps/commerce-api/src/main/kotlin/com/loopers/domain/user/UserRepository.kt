package com.loopers.domain.user

interface UserRepository {
    fun find(id: Long): UserModel?
    fun findByUsername(username: String): UserModel?
    fun save(user: UserModel): UserModel
}
