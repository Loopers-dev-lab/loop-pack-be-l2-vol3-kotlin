package com.loopers.domain.queue.token

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.queue.token.model.EntryTokenConsumeResult
import com.loopers.domain.queue.token.repository.EntryTokenRepository

class FakeEntryTokenRepository : EntryTokenRepository {

    private val store = java.util.concurrent.ConcurrentHashMap<UserId, String>()

    override fun issue(userId: UserId, token: String, ttlSeconds: Long) {
        store[userId] = token
    }

    override fun find(userId: UserId): String? = store[userId]

    override fun delete(userId: UserId) {
        store.remove(userId)
    }

    override fun consumeIfValid(userId: UserId, token: String): EntryTokenConsumeResult {
        val stored = store[userId] ?: return EntryTokenConsumeResult.NOT_FOUND
        if (stored != token) return EntryTokenConsumeResult.MISMATCH
        store.remove(userId, token)
        return EntryTokenConsumeResult.SUCCESS
    }
}
