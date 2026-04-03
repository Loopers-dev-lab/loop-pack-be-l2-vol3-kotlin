package com.loopers.domain.queue

interface EntryTokenRepository {

    fun issue(userId: Long, token: String, ttlSeconds: Long)

    fun get(userId: Long): String?

    /**
     * 저장된 토큰과 일치하면 원자적으로 삭제하고 true를 반환한다.
     * 토큰이 없거나 불일치하면 false를 반환한다.
     */
    fun consumeIfMatches(userId: Long, token: String): Boolean
}
