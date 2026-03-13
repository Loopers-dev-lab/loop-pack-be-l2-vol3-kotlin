package com.loopers.infrastructure.product

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.product.ProductCacheSnapshot
import com.loopers.application.product.ProductCacheStore
import com.loopers.config.redis.RedisConfig
import com.loopers.domain.product.ProductSortType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class ProductCacheStoreImpl(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) : ProductCacheStore {
    companion object {
        private const val DETAIL_KEY_PREFIX = "product:detail"
        private const val LIST_KEY_PREFIX = "product:list"
        private val DETAIL_TTL: Duration = Duration.ofMinutes(10)
        private val LIST_TTL: Duration = Duration.ofMinutes(3)
    }

    override fun getProductDetail(productId: Long): ProductCacheSnapshot? {
        val cached = redisTemplate.opsForValue().get(detailKey(productId)) ?: return null
        return objectMapper.readValue(cached, ProductCacheSnapshot::class.java)
    }

    override fun putProductDetail(product: ProductCacheSnapshot) {
        val key = detailKey(product.id)
        val value = objectMapper.writeValueAsString(product)
        redisTemplate.opsForValue().set(key, value, DETAIL_TTL)
    }

    override fun getProductList(brandId: Long?, sortType: ProductSortType, pageable: Pageable): Page<ProductCacheSnapshot>? {
        val cached = redisTemplate.opsForValue().get(listKey(brandId, sortType, pageable)) ?: return null
        val payload = objectMapper.readValue(cached, CachedProductPage::class.java)
        return PageImpl(
            payload.content,
            PageRequest.of(payload.pageNumber, payload.pageSize),
            payload.totalElements,
        )
    }

    override fun putProductList(
        brandId: Long?,
        sortType: ProductSortType,
        pageable: Pageable,
        products: Page<ProductCacheSnapshot>,
    ) {
        val key = listKey(brandId, sortType, pageable)
        val payload = CachedProductPage(
            content = products.content,
            pageNumber = products.number,
            pageSize = products.size,
            totalElements = products.totalElements,
        )
        val value = objectMapper.writeValueAsString(payload)
        redisTemplate.opsForValue().set(key, value, LIST_TTL)
    }

    override fun evictProductDetail(productId: Long) {
        redisTemplate.delete(detailKey(productId))
    }

    override fun evictProductList() {
        val keys = redisTemplate.keys("$LIST_KEY_PREFIX:*")
        if (keys.isNotEmpty()) {
            redisTemplate.delete(keys)
        }
    }

    private fun detailKey(productId: Long): String = "$DETAIL_KEY_PREFIX:$productId"

    private fun listKey(brandId: Long?, sortType: ProductSortType, pageable: Pageable): String {
        val brandKey = brandId?.toString() ?: "all"
        return "$LIST_KEY_PREFIX:$brandKey:${sortType.value}:${pageable.pageNumber}:${pageable.pageSize}"
    }

    private data class CachedProductPage(
        val content: List<ProductCacheSnapshot>,
        val pageNumber: Int,
        val pageSize: Int,
        val totalElements: Long,
    )
}
