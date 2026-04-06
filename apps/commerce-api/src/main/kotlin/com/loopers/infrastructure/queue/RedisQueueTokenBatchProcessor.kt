package com.loopers.infrastructure.queue

import com.loopers.config.redis.RedisConfig.Companion.REDIS_TEMPLATE_MASTER
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.queue.QueueTokenBatchProcessor
import com.loopers.domain.queue.token.model.EntryToken
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Component

@Component
class RedisQueueTokenBatchProcessor(
    @param:Qualifier(REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) : QueueTokenBatchProcessor {

    companion object {
        /**
         * Lua 스크립트: 대기열 앞에서 N명을 꺼내고 각 사용자 토큰을 원자적으로 발급한다.
         *
         * KEYS[1] = waiting-queue
         * KEYS[2] = entry-token: (토큰 키 접두사)
         * ARGV[1] = count
         * ARGV[2] = ttlSeconds
         *
         * 반환: [userId1, token1, userId2, token2, ...]
         */
        private val POP_AND_ISSUE_SCRIPT = RedisScript.of(
            """
            local count = tonumber(ARGV[1])
            if count <= 0 then
                return {}
            end

            local popped = redis.call('ZPOPMIN', KEYS[1], count)
            if #popped == 0 then
                return {}
            end

            local time = redis.call('TIME')
            local issued = {}

            for i = 1, #popped, 2 do
                local userId = popped[i]
                local token = redis.sha1hex(userId .. ':' .. time[1] .. ':' .. time[2] .. ':' .. i)
                redis.call('SET', KEYS[2] .. userId, token, 'EX', ARGV[2])
                table.insert(issued, userId)
                table.insert(issued, token)
            end

            return issued
            """.trimIndent(),
            List::class.java,
        )
    }

    override fun popAndIssueTokens(count: Int, ttlSeconds: Long): List<EntryToken> {
        @Suppress("UNCHECKED_CAST")
        val rawResult = redisTemplate.execute(
            POP_AND_ISSUE_SCRIPT,
            listOf(RedisQueueConstants.QUEUE_KEY, RedisQueueConstants.TOKEN_KEY_PREFIX),
            count.toString(),
            ttlSeconds.toString(),
        ) as? List<*> ?: return emptyList()

        if (rawResult.size % 2 != 0) {
            throw CoreException(ErrorType.INTERNAL_ERROR, "토큰 배치 발급 Lua 스크립트 예상 밖 반환 개수: ${rawResult.size}")
        }

        return rawResult.chunked(2).map { pair ->
            val userIdValue = pair[0] as? String
                ?: throw CoreException(ErrorType.INTERNAL_ERROR, "토큰 배치 발급 Lua 스크립트 userId 타입 오류: ${pair[0]}")
            val token = pair[1] as? String
                ?: throw CoreException(ErrorType.INTERNAL_ERROR, "토큰 배치 발급 Lua 스크립트 token 타입 오류: ${pair[1]}")
            val userId = userIdValue.toLongOrNull()
                ?: throw CoreException(ErrorType.INTERNAL_ERROR, "토큰 배치 발급 Lua 스크립트 userId 값 오류: $userIdValue")

            EntryToken(
                userId = UserId(userId),
                token = token,
            )
        }
    }
}
