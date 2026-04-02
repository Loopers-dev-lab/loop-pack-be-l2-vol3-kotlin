package com.loopers.domain.queue

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * 인메모리 토큰 Fake.
 * Redis String + TTL 동작을 모사한다.
 * - save: 토큰 저장 + 만료 시각 기록
 * - get: 만료 전이면 반환, 만료되었으면 삭제 후 null
 * - validateAndConsume: 원자적 검증 + 삭제
 */
class FakeTokenRepository : TokenRepository {

    data class TokenEntry(val token: String, val expiresAt: Instant)

    private val store = ConcurrentHashMap<Long, TokenEntry>()

    override fun save(userId: Long, token: String, ttlSeconds: Long) {
        val expiresAt = Instant.now().plusSeconds(ttlSeconds)
        store[userId] = TokenEntry(token, expiresAt)
    }

    override fun saveAll(tokens: Map<Long, String>, ttlSeconds: Long) {
        tokens.forEach { (userId, token) -> save(userId, token, ttlSeconds) }
    }

    override fun get(userId: Long): String? {
        val entry = store[userId] ?: return null
        if (Instant.now().isAfter(entry.expiresAt)) {
            store.remove(userId)
            return null
        }
        return entry.token
    }

    @Synchronized
    override fun validateAndConsume(userId: Long, token: String): Boolean {
        val entry = store[userId] ?: return false
        if (Instant.now().isAfter(entry.expiresAt)) {
            store.remove(userId)
            return false
        }
        if (entry.token != token) return false
        store.remove(userId)
        return true
    }

    override fun activeCount(): Long {
        // 만료된 것 제거 후 카운트
        val now = Instant.now()
        store.entries.removeIf { now.isAfter(it.value.expiresAt) }
        return store.size.toLong()
    }

    fun clear() {
        store.clear()
    }
}
