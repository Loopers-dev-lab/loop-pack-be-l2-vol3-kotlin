package com.loopers.domain.user

import com.loopers.domain.BaseEntity
import com.loopers.domain.user.entity.User
import com.loopers.domain.user.repository.UserRepository

class FakeUserRepository : UserRepository {

    private val users = mutableListOf<User>()
    private var sequence = 1L

    override fun save(user: User): User {
        if (user.id != 0L) {
            users.removeIf { it.id == user.id }
            users.add(user)
        } else {
            setEntityId(user, sequence++)
            users.add(user)
        }
        return user
    }

    override fun findById(id: Long): User? {
        return users.find { it.id == id }
    }

    override fun findByLoginId(loginId: String): User? {
        return users.find { it.loginId == loginId }
    }

    override fun existsByLoginId(loginId: String): Boolean {
        return users.any { it.loginId == loginId }
    }

    private fun setEntityId(entity: BaseEntity, id: Long) {
        BaseEntity::class.java.getDeclaredField("id").apply {
            isAccessible = true
            set(entity, id)
        }
    }
}
