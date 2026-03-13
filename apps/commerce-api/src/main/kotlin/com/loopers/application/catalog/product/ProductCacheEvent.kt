package com.loopers.application.catalog.product

import com.loopers.domain.catalog.product.model.Product
import com.loopers.domain.common.vo.BrandId
import com.loopers.domain.common.vo.ProductId

sealed interface ProductCacheEvent {
    /** 상품 상세 캐시 저장 + 선택적 목록 캐시 evict */
    data class DetailUpdated(
        val product: Product,
        val evictList: Boolean = false,
    ) : ProductCacheEvent

    /** 상품 상세 캐시 삭제 + 목록 캐시 evict */
    data class DetailEvicted(
        val productId: ProductId,
        val brandId: BrandId?,
    ) : ProductCacheEvent
}
