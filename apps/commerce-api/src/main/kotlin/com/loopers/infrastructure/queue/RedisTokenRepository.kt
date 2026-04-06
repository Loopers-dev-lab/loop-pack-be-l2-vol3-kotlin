package com.loopers.infrastructure.queue

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.queue.TokenRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.connection.RedisStringCommands.SetOption
import org.springframework.data.redis.core.types.Expiration
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Repository
import java.time.Duration

/**
 * Redis String + TTL 기반 입장 토큰 구현체.
 *
 * 토큰 소비(validateAndConsume)는 Lua 스크립트로 원자적으로 처리한다.
 * → GET + 비교 + DEL을 하나의 명령으로 묶어 race condition 방지.
 *
 * 활성 토큰 수 추적:
 * - 토큰 발급 시 INCR, 소비/삭제 시 DECR로 별도 카운터를 관리한다.
 * - TTL 만료 시에는 카운터가 자동으로 줄지 않으므로, 정확한 값은 아니다.
 *   → processQueue에서 maxActiveTokens와 비교할 때 "대략적인 상한"으로 사용.
 *   → 정확성보다 성능(SCAN 회피)을 우선.
 */
@Repository
class RedisTokenRepository(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) : TokenRepository {

    override fun save(userId: Long, token: String, ttlSeconds: Long) {
        val key = QueueRedisKeys.tokenKey(userId)
        redisTemplate.opsForValue().set(key, token, Duration.ofSeconds(ttlSeconds))
        redisTemplate.opsForValue().increment(QueueRedisKeys.ACTIVE_TOKEN_COUNT)
    }

    override fun get(userId: Long): String? {
        return redisTemplate.opsForValue().get(QueueRedisKeys.tokenKey(userId))
    }

    /**
     * 토큰 검증 + 소비를 원자적으로 수행한다.
     *
     * Lua 스크립트:
     * 1. GET으로 저장된 토큰 조회
     * 2. 전달받은 토큰과 비교
     * 3. 일치하면 DEL + 카운터 DECR + return 1
     * 4. 불일치/만료면 return 0
     *
     * → 동일 유저의 동시 주문 요청에서 하나만 통과.
     */
    override fun validateAndConsume(userId: Long, token: String): Boolean {
        val result = redisTemplate.execute(
            VALIDATE_AND_CONSUME_SCRIPT,
            listOf(QueueRedisKeys.tokenKey(userId), QueueRedisKeys.ACTIVE_TOKEN_COUNT),
            token,
        )
        return result == 1L
    }

    /**
     * 여러 토큰을 Pipeline으로 일괄 발급한다.
     *
     * 일반 save() N번 호출: 네트워크 왕복(RTT) N번 발생.
     * Pipeline: N개 명령을 한 번에 전송 → RTT 1번으로 감소.
     * 실무 환경(별도 Redis 서버, RTT 1~5ms)에서 유효한 최적화.
     *
     * active-count는 개별 INCR 대신 INCRBY(N)으로 한 번에 증가.
     */
    override fun saveAll(tokens: Map<Long, String>, ttlSeconds: Long) {
        if (tokens.isEmpty()) return
        val expiration = Expiration.seconds(ttlSeconds)
        redisTemplate.executePipelined { connection ->
            tokens.forEach { (userId, token) ->
                connection.stringCommands().set(
                    QueueRedisKeys.tokenKey(userId).toByteArray(),
                    token.toByteArray(),
                    expiration,
                    SetOption.upsert(),
                )
            }
            connection.stringCommands()
                .incrBy(QueueRedisKeys.ACTIVE_TOKEN_COUNT.toByteArray(), tokens.size.toLong())
            null
        }
    }

    override fun activeCount(): Long {
        val count = redisTemplate.opsForValue().get(QueueRedisKeys.ACTIVE_TOKEN_COUNT)
        return count?.toLongOrNull()?.coerceAtLeast(0) ?: 0L
    }

    companion object {
        private val VALIDATE_AND_CONSUME_SCRIPT = RedisScript.of<Long>(
            """
            local token = redis.call('GET', KEYS[1])
            if token == ARGV[1] then
                redis.call('DEL', KEYS[1])
                redis.call('DECR', KEYS[2])
                return 1
            end
            return 0
            """.trimIndent(),
            Long::class.java,
        )
    }
}
