package com.loopers.domain.queue.token.repository

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.queue.token.model.EntryTokenConsumeResult

interface EntryTokenRepository {

    /**
     * 입장 토큰을 발급한다 (Redis SET EX).
     */
    fun issue(userId: UserId, token: String, ttlSeconds: Long)

    /**
     * 입장 토큰을 조회한다.
     *
     * @return 토큰 값. 없거나 만료 시 null.
     */
    fun find(userId: UserId): String?

    /**
     * 입장 토큰을 삭제한다.
     */
    fun delete(userId: UserId)

    /**
     * 토큰을 원자적으로 검증하고 소비(삭제)한다.
     * GET + 비교 + DEL이 단일 원자 연산으로 수행된다.
     */
    fun consumeIfValid(userId: UserId, token: String): EntryTokenConsumeResult
}
