package com.loopers.application.handler.command.cache

import com.loopers.application.product.ProductCacheStore
import com.loopers.domain.common.command.EvictProductCacheCommand
import org.springframework.stereotype.Component

@Component
class ProductCacheCommandHandler(
    private val productCacheStore: ProductCacheStore,
) {
    fun handle(command: EvictProductCacheCommand) {
        productCacheStore.evictProduct(command.productId)
    }
}
