package com.loopers.infrastructure.order

import com.loopers.config.redis.RedisConfig.Companion.REDIS_TEMPLATE_MASTER
import com.loopers.config.redis.RedisKeys
import com.loopers.domain.order.StockReservationRepository
import com.loopers.domain.product.ProductRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.ClassPathResource
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Component

@Component
class StockReservationRedisRepository(
    @Qualifier(REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
    private val productRepository: ProductRepository,
) : StockReservationRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    private val reserveScript = RedisScript.of<Long>(
        ClassPathResource("scripts/stock_reserve.lua"),
        Long::class.java,
    )

    override fun reserve(productId: Long, quantity: Int): Boolean {
        return try {
            val result = masterRedisTemplate.execute(
                reserveScript,
                listOf(RedisKeys.stockKey(productId)),
                quantity.toString(),
            ) ?: 0L
            result == 1L
        } catch (e: RedisConnectionFailureException) {
            log.warn("[StockReservation] Redis 연결 실패, DB fallback 전환: productId={}", productId, e)
            fallbackToDbLock(productId, quantity)
        }
    }

    override fun restore(productId: Long, quantity: Int) {
        try {
            masterRedisTemplate.opsForValue()
                .increment(RedisKeys.stockKey(productId), quantity.toLong())
        } catch (e: RedisConnectionFailureException) {
            log.warn("[StockReservation] Redis 연결 실패, restore 스킵: productId={}", productId, e)
        }
    }

    override fun setStock(productId: Long, quantity: Int) {
        masterRedisTemplate.opsForValue()
            .set(RedisKeys.stockKey(productId), quantity.toString())
    }

    /**
     * Redis 장애 시 DB 비관적 락으로 재고 가용 여부만 확인한다.
     * 실제 DB 재고 차감은 consumer가 처리한다.
     */
    private fun fallbackToDbLock(productId: Long, quantity: Int): Boolean {
        val product = productRepository.findByIdWithLock(productId) ?: return false
        return product.stockQuantity.value >= quantity
    }
}
