package com.loopers.domain.user

class FakeUserRepository : UserRepository {

    private val store = mutableListOf<User>()

    override fun save(user: User): User {
        store.add(user)
        return user
    }

    override fun findByLoginId(loginId: String): User? {
        return store.find { it.loginId == loginId }
    }
}
