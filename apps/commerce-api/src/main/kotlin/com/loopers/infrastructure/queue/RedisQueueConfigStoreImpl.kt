package com.loopers.infrastructure.queue

import com.loopers.application.queue.QueueConfigStore
import com.loopers.config.redis.RedisConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class RedisQueueConfigStoreImpl(
    private val redisTemplate: RedisTemplate<String, String>,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
) : QueueConfigStore {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val CONFIG_KEY = "queue:config:enabled"
    }

    override fun isEnabled(): Boolean {
        return try {
            redisTemplate.opsForValue().get(CONFIG_KEY) == "true"
        } catch (e: Exception) {
            log.warn("[QueueConfigStore] 대기열 활성 상태 조회 실패", e)
            false
        }
    }

    override fun setEnabled(enabled: Boolean) {
        try {
            masterRedisTemplate.opsForValue().set(CONFIG_KEY, enabled.toString())
        } catch (e: Exception) {
            log.warn("[QueueConfigStore] 대기열 활성 상태 변경 실패 (enabled={})", enabled, e)
        }
    }
}
