package com.loopers.domain.user

import com.loopers.domain.BaseEntity

class FakeUserRepository : UserRepository {

    private val store = mutableListOf<User>()
    private var idSequence = 1L

    override fun save(user: User): User {
        if (user.id == 0L) {
            setEntityId(user, idSequence++)
        }
        store.add(user)
        return user
    }

    override fun findByLoginId(loginId: String): User? {
        return store.find { it.loginId == loginId }
    }

    override fun findById(id: Long): User? {
        return store.find { it.id == id }
    }

    private fun setEntityId(entity: BaseEntity, id: Long) {
        val idField = BaseEntity::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
    }
}
