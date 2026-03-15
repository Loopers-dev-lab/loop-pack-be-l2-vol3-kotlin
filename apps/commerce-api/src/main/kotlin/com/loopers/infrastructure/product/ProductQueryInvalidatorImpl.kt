package com.loopers.infrastructure.product

import com.loopers.domain.product.ProductQueryInvalidator
import org.springframework.stereotype.Component

@Component
class ProductQueryInvalidatorImpl(
    private val redisProductQueryCache: RedisProductQueryCache,
) : ProductQueryInvalidator {
    override fun invalidateDetails(productIds: Collection<Long>) {
        redisProductQueryCache.evictDetails(productIds)
    }

    override fun invalidateListsByBrandId(brandId: Long) {
        redisProductQueryCache.invalidateListsByBrandId(brandId)
    }
}
