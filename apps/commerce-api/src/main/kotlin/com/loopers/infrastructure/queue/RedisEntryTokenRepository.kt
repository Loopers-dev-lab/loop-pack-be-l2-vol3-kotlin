package com.loopers.infrastructure.queue

import com.loopers.config.redis.RedisConfig.Companion.REDIS_TEMPLATE_MASTER
import com.loopers.domain.common.vo.UserId
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.domain.queue.token.repository.EntryTokenRepository
import com.loopers.domain.queue.token.model.EntryTokenConsumeResult
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class RedisEntryTokenRepository(
    @param:Qualifier(REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) : EntryTokenRepository {

    companion object {
        /**
         * Lua 스크립트: 원자적으로 토큰을 검증하고 소비한다.
         * GET + 비교 + 조건부 DEL을 단일 원자 연산으로 수행.
         *
         * KEYS[1] = entry-token:{userId}
         * ARGV[1] = 클라이언트가 전달한 토큰
         *
         * 반환: 1 (성공), 0 (미존재/만료), -1 (불일치)
         */
        private val CONSUME_SCRIPT = RedisScript.of(
            """
            local stored = redis.call('GET', KEYS[1])
            if stored == false then return 0 end
            if stored ~= ARGV[1] then return -1 end
            redis.call('DEL', KEYS[1])
            return 1
            """.trimIndent(),
            Long::class.java,
        )
    }

    private fun tokenKey(userId: UserId) = "${RedisQueueConstants.TOKEN_KEY_PREFIX}${userId.value}"

    override fun issue(userId: UserId, token: String, ttlSeconds: Long) {
        redisTemplate.opsForValue().set(
            tokenKey(userId),
            token,
            Duration.ofSeconds(ttlSeconds),
        )
    }

    override fun find(userId: UserId): String? {
        return redisTemplate.opsForValue().get(tokenKey(userId))
    }

    override fun delete(userId: UserId) {
        redisTemplate.delete(tokenKey(userId))
    }

    override fun consumeIfValid(userId: UserId, token: String): EntryTokenConsumeResult {
        val result = redisTemplate.execute(
            CONSUME_SCRIPT,
            listOf(tokenKey(userId)),
            token,
        )
        return when (result) {
            1L -> EntryTokenConsumeResult.SUCCESS
            0L -> EntryTokenConsumeResult.NOT_FOUND
            -1L -> EntryTokenConsumeResult.MISMATCH
            else -> throw CoreException(ErrorType.INTERNAL_ERROR, "토큰 소비 Lua 스크립트 예상 밖 반환값: $result")
        }
    }
}
