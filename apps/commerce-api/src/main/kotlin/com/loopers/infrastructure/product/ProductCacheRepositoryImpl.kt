package com.loopers.infrastructure.product

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.loopers.application.product.ProductCacheRepository
import com.loopers.application.product.ProductDetailInfo
import com.loopers.application.product.ProductInfo
import com.loopers.support.common.PageQuery
import com.loopers.support.common.PageResult
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class ProductCacheRepositoryImpl(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) : ProductCacheRepository {

    companion object {
        private const val PRODUCT_KEY_PREFIX = "product:detail:"
        private const val PRODUCT_LIST_KEY_PREFIX = "product:list:"
        private const val TTL_MINUTES = 10L
    }

    override fun getProduct(productId: Long): ProductDetailInfo? {
        val json = redisTemplate.opsForValue().get("$PRODUCT_KEY_PREFIX$productId") ?: return null
        return objectMapper.readValue<ProductDetailInfo>(json)
    }

    override fun setProduct(productId: Long, productDetailInfo: ProductDetailInfo) {
        val json = objectMapper.writeValueAsString(productDetailInfo)
        redisTemplate.opsForValue().set("$PRODUCT_KEY_PREFIX$productId", json, TTL_MINUTES, TimeUnit.MINUTES)
    }

    override fun evictProduct(productId: Long) {
        redisTemplate.delete("$PRODUCT_KEY_PREFIX$productId")
    }

    override fun getProducts(brandId: Long?, pageQuery: PageQuery): PageResult<ProductInfo>? {
        val key = buildListKey(brandId, pageQuery)
        val json = redisTemplate.opsForValue().get(key) ?: return null
        return objectMapper.readValue<PageResult<ProductInfo>>(json)
    }

    override fun setProducts(brandId: Long?, pageQuery: PageQuery, pageResult: PageResult<ProductInfo>) {
        val key = buildListKey(brandId, pageQuery)
        val json = objectMapper.writeValueAsString(pageResult)
        redisTemplate.opsForValue().set(key, json, TTL_MINUTES, TimeUnit.MINUTES)
    }

    override fun evictAllProducts() {
        val keys = redisTemplate.keys("$PRODUCT_LIST_KEY_PREFIX*")
        if (keys.isNotEmpty()) {
            redisTemplate.delete(keys)
        }
    }

    private fun buildListKey(brandId: Long?, pageQuery: PageQuery): String {
        val brand = brandId ?: "all"
        val sort = "${pageQuery.sort.property}:${pageQuery.sort.direction}"
        return "${PRODUCT_LIST_KEY_PREFIX}brand:$brand:$sort:${pageQuery.page}:${pageQuery.size}"
    }
}
