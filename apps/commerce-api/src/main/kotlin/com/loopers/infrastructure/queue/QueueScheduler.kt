package com.loopers.infrastructure.queue

import com.loopers.config.redis.RedisConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

@Component
class QueueScheduler(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${queue.scheduler.fixed-delay:100}")
    fun processQueue() {
        val lockValue = tryAcquireLock() ?: return
        try {
            val uuids = (1..BATCH_SIZE).map { UUID.randomUUID().toString() }
            val args = listOf(BATCH_SIZE.toString(), TOKEN_TTL_SECONDS) + uuids
            val results = redisTemplate.execute(
                POP_AND_ISSUE_SCRIPT,
                listOf(WAITING_QUEUE_KEY),
                *args.toTypedArray(),
            ) ?: emptyList<Any>()
            val processedCount = results.size / 2
            if (processedCount > 0) {
                log.info("Processed queue batch: {} users", processedCount)
            }
        } finally {
            releaseLock(lockValue)
        }
    }

    private fun tryAcquireLock(): String? {
        val value = UUID.randomUUID().toString()
        val acquired = redisTemplate.opsForValue()
            .setIfAbsent(LOCK_KEY, value, LOCK_LEASE) == true
        return if (acquired) value else null
    }

    private fun releaseLock(value: String) {
        runCatching {
            redisTemplate.execute(RELEASE_LOCK_SCRIPT, listOf(LOCK_KEY), value)
        }
    }

    companion object {
        const val BATCH_SIZE = 21
        private const val LOCK_KEY = "queue:scheduler:lock"

        // Phase 2 TODO: 락 갱신(renewal) 미지원. 500ms 내 배치 처리 완료를 전제.
        // 배치 크기 14 × Redis 명령 ~1ms = ~14ms로 충분한 여유.
        // 다중 인스턴스 환경에서 배치 크기 증가 시 재검토 필요.
        private val LOCK_LEASE = Duration.ofMillis(500)
        private const val WAITING_QUEUE_KEY = "queue:waiting"
        private const val TOKEN_TTL_SECONDS = "300"

        private val RELEASE_LOCK_SCRIPT = RedisScript.of<Long>(
            """
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """.trimIndent(),
            Long::class.javaObjectType,
        )

        private val POP_AND_ISSUE_SCRIPT = RedisScript.of<List<*>>(
            """
            local members = redis.call('ZPOPMIN', KEYS[1], ARGV[1])
            local results = {}
            local uuidIdx = 3
            for i = 1, #members, 2 do
                local userId = members[i]
                local token = ARGV[uuidIdx]
                redis.call('SET', 'queue:entry-token:' .. userId, token, 'EX', ARGV[2])
                table.insert(results, userId)
                table.insert(results, token)
                uuidIdx = uuidIdx + 1
            end
            return results
            """.trimIndent(),
            List::class.java,
        )
    }
}
