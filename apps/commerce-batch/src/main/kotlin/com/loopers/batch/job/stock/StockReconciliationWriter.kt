package com.loopers.batch.job.stock

import com.loopers.config.redis.RedisConfig.Companion.REDIS_TEMPLATE_MASTER
import com.loopers.config.redis.RedisKeys
import org.slf4j.LoggerFactory
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

/**
 * 불일치 항목의 Redis 재고를 DB 기준으로 보정한다.
 */
@Component
class StockReconciliationWriter(
    @Qualifier(REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
) : ItemWriter<ProductStock> {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun write(chunk: Chunk<out ProductStock>) {
        val stockMap = chunk.items.associate { item ->
            RedisKeys.stockKey(item.productId) to item.dbStock.toString()
        }
        masterRedisTemplate.opsForValue().multiSet(stockMap)
        log.info("[StockReconciliation] 보정 완료: {}건", chunk.items.size)
    }
}
