package com.loopers.infrastructure.queue

import com.loopers.application.queue.QueueStore
import com.loopers.config.redis.RedisConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class RedisQueueStoreImpl(
    private val redisTemplate: RedisTemplate<String, String>,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
) : QueueStore {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val QUEUE_KEY = "queue:waiting"
    }

    override fun add(memberId: Long, score: Double): Boolean {
        return try {
            masterRedisTemplate.opsForZSet().addIfAbsent(QUEUE_KEY, memberId.toString(), score) ?: false
        } catch (e: Exception) {
            log.warn("[QueueStore] 대기열 진입 실패 (memberId={})", memberId, e)
            false
        }
    }

    override fun rank(memberId: Long): Long? {
        return try {
            redisTemplate.opsForZSet().rank(QUEUE_KEY, memberId.toString())
        } catch (e: Exception) {
            log.warn("[QueueStore] 순번 조회 실패 (memberId={})", memberId, e)
            null
        }
    }

    override fun rankFromMaster(memberId: Long): Long? {
        return try {
            masterRedisTemplate.opsForZSet().rank(QUEUE_KEY, memberId.toString())
        } catch (e: Exception) {
            log.warn("[QueueStore] 순번 조회(master) 실패 (memberId={})", memberId, e)
            null
        }
    }

    override fun size(): Long {
        return try {
            redisTemplate.opsForZSet().size(QUEUE_KEY) ?: 0L
        } catch (e: Exception) {
            log.warn("[QueueStore] 대기열 크기 조회 실패", e)
            0L
        }
    }

    override fun popMin(count: Long): List<Long> {
        return try {
            val result = masterRedisTemplate.opsForZSet().popMin(QUEUE_KEY, count)
            result?.mapNotNull { it.value?.toLongOrNull() } ?: emptyList()
        } catch (e: Exception) {
            log.warn("[QueueStore] 대기열 popMin 실패 (count={})", count, e)
            emptyList()
        }
    }

    override fun remove(memberId: Long): Boolean {
        return try {
            val removed = masterRedisTemplate.opsForZSet().remove(QUEUE_KEY, memberId.toString())
            (removed ?: 0) > 0
        } catch (e: Exception) {
            log.warn("[QueueStore] 대기열 제거 실패 (memberId={})", memberId, e)
            false
        }
    }
}
