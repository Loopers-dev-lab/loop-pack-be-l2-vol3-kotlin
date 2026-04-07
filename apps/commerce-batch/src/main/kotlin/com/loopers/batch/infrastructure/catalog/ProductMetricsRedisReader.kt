package com.loopers.batch.infrastructure.catalog

import com.loopers.config.redis.RedisConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ScanOptions
import org.springframework.stereotype.Component

@Component
class ProductMetricsRedisReader(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) {
    private val log = LoggerFactory.getLogger(ProductMetricsRedisReader::class.java)

    companion object {
        private const val KEY_PREFIX = "product:metrics"
    }

    /**
     * 모든 product:metrics:* 키를 스캔하여 메트릭을 읽고, 읽은 만큼 차감한다.
     * 배치 단일 인스턴스 실행을 전제하므로 HGETALL → HINCRBY(-value) 순서로 처리한다.
     *
     * @return Map<productId, Map<field, value>>
     */
    fun readAllAndReset(): Map<Long, Map<String, Long>> {
        val result = mutableMapOf<Long, Map<String, Long>>()

        val keys = mutableListOf<String>()
        redisTemplate.scan(
            ScanOptions.scanOptions()
                .match("$KEY_PREFIX:*")
                .count(100)
                .build(),
        ).use { cursor -> cursor.forEach { keys.add(it) } }

        val ops = redisTemplate.opsForHash<String, String>()

        for (key in keys) {
            val productId = key.removePrefix("$KEY_PREFIX:").toLongOrNull() ?: continue
            val entries = ops.entries(key)
            if (entries.isEmpty()) continue

            val metrics = mutableMapOf<String, Long>()
            for ((field, value) in entries) {
                val longValue = value.toLongOrNull() ?: 0L
                if (longValue != 0L) {
                    metrics[field] = longValue
                    ops.increment(key, field, -longValue)
                }
            }

            if (metrics.isNotEmpty()) {
                result[productId] = metrics
                log.debug("productId={}, metrics={}", productId, metrics)
            }
        }

        log.info("Redis에서 {}개 상품 메트릭을 읽었습니다.", result.size)
        return result
    }
}
