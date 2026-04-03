package com.loopers.domain.queue.fixture

import com.loopers.domain.queue.EntryTokenRepository

class FakeEntryTokenRepository : EntryTokenRepository {

    private val tokens = mutableMapOf<Long, String>()

    override fun issueToken(userId: Long, token: String, ttlSeconds: Long) {
        tokens[userId] = token
    }

    override fun findToken(userId: Long): String? = tokens[userId]

    override fun deleteToken(userId: Long) {
        tokens.remove(userId)
    }

    override fun hasToken(userId: Long): Boolean = tokens.containsKey(userId)
}
