package com.loopers.domain.user

import com.loopers.domain.user.vo.LoginId

interface UserRepository {
    fun existsByLoginId(loginId: LoginId): Boolean
    fun save(user: User): User
    fun findUserById(id: Long): User?
    fun findByLoginId(loginId: LoginId): User?
}
