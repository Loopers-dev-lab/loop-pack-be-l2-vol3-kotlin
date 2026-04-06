package com.loopers.domain.queue

/**
 * 입장 토큰(Redis String + TTL) 추상화.
 * 토큰 발급, 검증+소비(원자적), 삭제를 담당한다.
 */
interface TokenRepository {

    /** 유저에게 입장 토큰을 발급한다. TTL(초) 이후 자동 만료. */
    fun save(userId: Long, token: String, ttlSeconds: Long)

    /**
     * 여러 유저에게 입장 토큰을 일괄 발급한다.
     * 구현체는 Pipeline으로 처리해 RTT를 최소화한다.
     * key: userId, value: token
     */
    fun saveAll(tokens: Map<Long, String>, ttlSeconds: Long)

    /** 유저의 토큰을 조회한다. 만료/미발급이면 null. */
    fun get(userId: Long): String?

    /**
     * 토큰을 검증하고 소비한다 (원자적).
     * 저장된 토큰과 일치하면 삭제 후 true, 불일치/만료면 false.
     */
    fun validateAndConsume(userId: Long, token: String): Boolean

    /** 현재 활성 토큰 수를 조회한다. */
    fun activeCount(): Long
}
