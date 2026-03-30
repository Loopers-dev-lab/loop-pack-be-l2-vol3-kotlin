package com.loopers.domain.queue.token

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.queue.token.repository.EntryTokenRepository

class FakeEntryTokenRepository : EntryTokenRepository {

    private val store = mutableMapOf<UserId, String>()

    override fun issue(userId: UserId, token: String, ttlSeconds: Long) {
        store[userId] = token
    }

    override fun find(userId: UserId): String? = store[userId]

    override fun delete(userId: UserId) {
        store.remove(userId)
    }
}
