package com.loopers.domain.queue.token.repository

import com.loopers.domain.common.vo.UserId

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
     * 입장 토큰을 삭제한다 (주문 완료 후).
     */
    fun delete(userId: UserId)
}
