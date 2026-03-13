package com.loopers.infrastructure.product

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.loopers.application.product.ProductCacheRepository
import com.loopers.application.product.ProductDetailInfo
import com.loopers.application.product.ProductInfo
import com.loopers.support.common.PageQuery
import com.loopers.support.common.PageResult
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.ScanOptions
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class ProductCacheRepositoryImpl(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) : ProductCacheRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun getProduct(productId: Long): ProductDetailInfo? {
        return getOrDeserialize(ProductCachePolicy.detailKey(productId), "product:$productId")
    }

    override fun setProduct(productId: Long, productDetailInfo: ProductDetailInfo) {
        runCatching {
            val json = objectMapper.writeValueAsString(productDetailInfo)
            redisTemplate.opsForValue().set(
                ProductCachePolicy.detailKey(productId),
                json,
                ProductCachePolicy.DETAIL_TTL,
            )
        }.onFailure { log.warn("Redis 캐시 저장 실패: product:{}", productId, it) }
    }

    override fun evictProduct(productId: Long) {
        runCatching {
            redisTemplate.delete(ProductCachePolicy.detailKey(productId))
        }.onFailure { log.warn("Redis 캐시 삭제 실패: product:{}", productId, it) }
    }

    override fun getProducts(brandId: Long?, pageQuery: PageQuery): PageResult<ProductInfo>? {
        return getOrDeserialize(ProductCachePolicy.listKey(brandId, pageQuery), "products:brand:$brandId")
    }

    override fun setProducts(brandId: Long?, pageQuery: PageQuery, pageResult: PageResult<ProductInfo>) {
        runCatching {
            val json = objectMapper.writeValueAsString(pageResult)
            redisTemplate.opsForValue().set(
                ProductCachePolicy.listKey(brandId, pageQuery),
                json,
                ProductCachePolicy.LIST_TTL,
            )
        }.onFailure { log.warn("Redis 캐시 저장 실패: products:brand:{}", brandId, it) }
    }

    override fun evictAllProducts() {
        runCatching {
            val scanOptions = ScanOptions.scanOptions()
                .match(ProductCachePolicy.listKeyPattern())
                .count(100)
                .build()
            redisTemplate.connectionFactory?.connection?.use { connection ->
                val cursor = connection.scan(scanOptions)
                val keys = mutableListOf<String>()
                cursor.forEachRemaining { keys.add(String(it)) }
                if (keys.isNotEmpty()) {
                    redisTemplate.delete(keys)
                }
            }
        }.onFailure { log.warn("Redis 캐시 전체 삭제 실패", it) }
    }

    private inline fun <reified T> getOrDeserialize(key: String, label: String): T? {
        val json = runCatching { redisTemplate.opsForValue().get(key) }
            .onFailure { log.warn("Redis 캐시 조회 실패: {}", label, it) }
            .getOrNull() ?: return null

        return runCatching { objectMapper.readValue<T>(json) }
            .onFailure {
                log.warn("Redis 캐시 역직렬화 실패, 손상 키 삭제: {}", label, it)
                runCatching { redisTemplate.delete(key) }
            }.getOrNull()
    }
}
