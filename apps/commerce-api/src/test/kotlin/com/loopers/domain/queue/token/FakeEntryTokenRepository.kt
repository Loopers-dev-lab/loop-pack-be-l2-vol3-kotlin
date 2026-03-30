package com.loopers.domain.queue.token

import com.loopers.domain.queue.token.repository.EntryTokenRepository

class FakeEntryTokenRepository : EntryTokenRepository {

    private val store = mutableMapOf<Long, String>()

    override fun issue(userId: Long, token: String, ttlSeconds: Long) {
        store[userId] = token
    }

    override fun find(userId: Long): String? = store[userId]

    override fun delete(userId: Long) {
        store.remove(userId)
    }
}
