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

@Component
class ProductCacheRepositoryImpl(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) : ProductCacheRepository {

    override fun getProduct(productId: Long): ProductDetailInfo? {
        val json = redisTemplate.opsForValue().get(ProductCachePolicy.detailKey(productId)) ?: return null
        return objectMapper.readValue<ProductDetailInfo>(json)
    }

    override fun setProduct(productId: Long, productDetailInfo: ProductDetailInfo) {
        val json = objectMapper.writeValueAsString(productDetailInfo)
        redisTemplate.opsForValue().set(
            ProductCachePolicy.detailKey(productId),
            json,
            ProductCachePolicy.DETAIL_TTL,
        )
    }

    override fun evictProduct(productId: Long) {
        redisTemplate.delete(ProductCachePolicy.detailKey(productId))
    }

    override fun getProducts(brandId: Long?, pageQuery: PageQuery): PageResult<ProductInfo>? {
        val json = redisTemplate.opsForValue().get(ProductCachePolicy.listKey(brandId, pageQuery)) ?: return null
        return objectMapper.readValue<PageResult<ProductInfo>>(json)
    }

    override fun setProducts(brandId: Long?, pageQuery: PageQuery, pageResult: PageResult<ProductInfo>) {
        val json = objectMapper.writeValueAsString(pageResult)
        redisTemplate.opsForValue().set(
            ProductCachePolicy.listKey(brandId, pageQuery),
            json,
            ProductCachePolicy.LIST_TTL,
        )
    }

    override fun evictAllProducts() {
        val keys = redisTemplate.keys(ProductCachePolicy.listKeyPattern())
        if (keys.isNotEmpty()) {
            redisTemplate.delete(keys)
        }
    }
}
