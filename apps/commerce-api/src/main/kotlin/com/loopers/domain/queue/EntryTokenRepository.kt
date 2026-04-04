package com.loopers.domain.queue

interface EntryTokenRepository {

    /**
     * @return true: 발급 성공, false: 발급 실패 (Redis 장애 등)
     */
    fun issue(userId: Long, token: String, ttlSeconds: Long): Boolean

    fun get(userId: Long): String?

    /**
     * 저장된 토큰과 일치하면 원자적으로 삭제하고 true를 반환한다.
     * 토큰이 없거나 불일치하면 false를 반환한다.
     */
    fun consumeIfMatches(userId: Long, token: String): Boolean
}
