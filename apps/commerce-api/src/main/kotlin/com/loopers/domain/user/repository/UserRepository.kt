package com.loopers.domain.user.repository

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.user.model.User

interface UserRepository {
    fun save(user: User): User
    fun findById(id: UserId): User?
    fun findByLoginId(loginId: String): User?
    fun existsByLoginId(loginId: String): Boolean
}
