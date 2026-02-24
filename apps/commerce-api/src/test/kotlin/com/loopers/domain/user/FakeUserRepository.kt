package com.loopers.domain.user

import com.loopers.domain.user.model.User
import com.loopers.domain.user.repository.UserRepository

class FakeUserRepository : UserRepository {

    private val users = mutableListOf<User>()
    private var sequence = 1L

    override fun save(user: User): User {
        if (user.id != 0L) {
            users.removeIf { it.id == user.id }
            users.add(user)
            return user
        }
        val saved = User(
            id = sequence++,
            loginId = user.loginId,
            password = user.password,
            name = user.name,
            birthDate = user.birthDate,
            email = user.email,
            deletedAt = user.deletedAt,
        )
        users.add(saved)
        return saved
    }

    override fun findById(id: Long): User? {
        return users.find { it.id == id }
    }

    override fun findByLoginId(loginId: String): User? {
        return users.find { it.loginId.value == loginId }
    }

    override fun existsByLoginId(loginId: String): Boolean {
        return users.any { it.loginId.value == loginId }
    }
}
