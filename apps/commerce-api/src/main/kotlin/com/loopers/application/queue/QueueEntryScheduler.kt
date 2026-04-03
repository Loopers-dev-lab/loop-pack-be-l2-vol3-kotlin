package com.loopers.application.queue

import com.loopers.config.redis.RedisConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

@Component
class QueueEntryScheduler(
    private val queueService: QueueService,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val LOCK_KEY = "queue:scheduler:lock"
        private val LOCK_TTL = Duration.ofSeconds(5)
        private val RELEASE_LOCK_SCRIPT = DefaultRedisScript<Long>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long::class.java,
        )
    }

    @Scheduled(fixedDelayString = "#{T(java.lang.Long).parseLong('\${queue.scheduler-interval-seconds:3}') * 1000}")
    fun schedule() {
        if (!queueService.isEnabled()) return

        val lockValue = UUID.randomUUID().toString()
        val acquired = masterRedisTemplate.opsForValue()
            .setIfAbsent(LOCK_KEY, lockValue, LOCK_TTL) ?: false

        if (!acquired) return

        try {
            queueService.processQueue()
        } catch (e: Exception) {
            log.error("[QueueScheduler] 대기열 처리 실패", e)
        } finally {
            masterRedisTemplate.execute(RELEASE_LOCK_SCRIPT, listOf(LOCK_KEY), lockValue)
        }
    }
}
