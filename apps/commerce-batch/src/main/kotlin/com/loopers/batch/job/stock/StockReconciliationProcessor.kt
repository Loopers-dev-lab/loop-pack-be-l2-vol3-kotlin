package com.loopers.batch.job.stock

import com.loopers.config.redis.RedisConfig.Companion.REDIS_TEMPLATE_MASTER
import com.loopers.config.redis.RedisKeys
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

/**
 * Redis 재고와 DB 재고를 비교하여 불일치 항목만 통과시킨다.
 * 일치하면 null을 반환하여 Writer로 전달하지 않는다.
 */
@Component
class StockReconciliationProcessor(
    @Qualifier(REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
) : ItemProcessor<ProductStock, ProductStock> {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun process(item: ProductStock): ProductStock? {
        val redisStock = masterRedisTemplate.opsForValue()
            .get(RedisKeys.stockKey(item.productId))?.toLongOrNull()

        if (redisStock != null && redisStock == item.dbStock) {
            return null
        }

        log.info(
            "[StockReconciliation] 불일치 감지: productId={}, Redis={}, DB={}",
            item.productId,
            redisStock,
            item.dbStock,
        )
        return item
    }
}
