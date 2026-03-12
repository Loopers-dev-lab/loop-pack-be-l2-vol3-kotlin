package com.loopers.infrastructure.product

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.loopers.application.product.ProductCachePort
import com.loopers.application.product.ProductInfo
import com.loopers.config.redis.RedisConfig
import com.loopers.domain.product.ProductSortType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class ProductCacheAdapter(
    private val redisTemplate: RedisTemplate<String, String>,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) : ProductCachePort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun getProductDetail(id: Long): ProductInfo? {
        return try {
            redisTemplate.opsForValue().get(detailKey(id))
                ?.let { objectMapper.readValue<ProductInfo>(it) }
        } catch (e: Exception) {
            log.warn("Redis 조회 실패 (product:detail:{}): {}", id, e.message)
            null
        }
    }

    override fun setProductDetail(id: Long, productInfo: ProductInfo) {
        try {
            val json = objectMapper.writeValueAsString(productInfo)
            masterRedisTemplate.opsForValue().set(detailKey(id), json, DETAIL_TTL)
        } catch (e: Exception) {
            log.warn("Redis 캐시 저장 실패 (product:detail:{}): {}", id, e.message)
        }
    }

    override fun evictProductDetail(id: Long) {
        try {
            masterRedisTemplate.delete(detailKey(id))
        } catch (e: Exception) {
            log.warn("Redis 캐시 삭제 실패 (product:detail:{}): {}", id, e.message)
        }
    }

    override fun getProductList(brandId: Long?, sortType: ProductSortType): List<ProductInfo>? {
        return try {
            redisTemplate.opsForValue().get(listKey(brandId, sortType))
                ?.let { objectMapper.readValue<List<ProductInfo>>(it) }
        } catch (e: Exception) {
            log.warn("Redis 조회 실패 (product:list): {}", e.message)
            null
        }
    }

    override fun setProductList(brandId: Long?, sortType: ProductSortType, list: List<ProductInfo>) {
        try {
            val json = objectMapper.writeValueAsString(list)
            masterRedisTemplate.opsForValue().set(listKey(brandId, sortType), json, LIST_TTL)
        } catch (e: Exception) {
            log.warn("Redis 캐시 저장 실패 (product:list): {}", e.message)
        }
    }

    private fun detailKey(id: Long) = "product:detail:$id"

    private fun listKey(brandId: Long?, sortType: ProductSortType): String {
        val brand = brandId?.toString() ?: "all"
        return "product:list:brand:$brand:sort:${sortType.name}"
    }

    companion object {
        private val DETAIL_TTL = Duration.ofMinutes(10)
        private val LIST_TTL = Duration.ofMinutes(5)
    }
}
