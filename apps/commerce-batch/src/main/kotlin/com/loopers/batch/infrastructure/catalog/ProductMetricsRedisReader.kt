package com.loopers.batch.infrastructure.catalog

import com.loopers.config.redis.RedisConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class ProductMetricsRedisReader(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) {
    private val log = LoggerFactory.getLogger(ProductMetricsRedisReader::class.java)

    companion object {
        private const val KEY_PREFIX = "rank"
        private const val TYPE_VIEW = "view"
        private const val TYPE_LIKE = "like"
        private const val TYPE_ORDER = "order"
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    /**
     * 지정 날짜의 rank:view/like/order ZSET을 스냅샷으로 읽어 productId별로 집계한다.
     * Redis가 SoT이므로 차감/수정 없이 읽기만 수행한다.
     * rank:all은 view/like/order에서 가중치로 재계산 가능하므로 백업 대상이 아니다.
     *
     * @return Map<productId, Map<field, value>>
     */
    fun readSnapshot(date: LocalDate): Map<Long, Map<String, Long>> {
        val viewMap = readZSet(TYPE_VIEW, date)
        val likeMap = readZSet(TYPE_LIKE, date)
        val orderMap = readZSet(TYPE_ORDER, date)

        val productIds = viewMap.keys + likeMap.keys + orderMap.keys
        val result = productIds.associateWith { productId ->
            val metrics = mutableMapOf<String, Long>()
            viewMap[productId]?.let { metrics["viewCount"] = it }
            likeMap[productId]?.let { metrics["likeCount"] = it }
            orderMap[productId]?.let { metrics["orderCount"] = it }
            metrics
        }

        log.info("date={}, {}개 상품 메트릭을 ZSET에서 읽었습니다.", date, result.size)
        return result
    }

    private fun readZSet(type: String, date: LocalDate): Map<Long, Long> {
        val key = key(type, date)
        val tuples = redisTemplate.opsForZSet().rangeWithScores(key, 0, -1) ?: return emptyMap()

        val result = mutableMapOf<Long, Long>()
        for (tuple in tuples) {
            val productId = tuple.value?.toLongOrNull() ?: continue
            val score = tuple.score?.toLong() ?: continue
            if (score != 0L) {
                result[productId] = score
            }
        }
        return result
    }

    private fun key(type: String, date: LocalDate): String =
        "$KEY_PREFIX:$type:${date.format(DATE_FORMAT)}"
}
