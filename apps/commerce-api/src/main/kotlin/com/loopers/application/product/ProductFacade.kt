package com.loopers.application.product

import com.loopers.support.common.PageQuery
import com.loopers.support.common.PageResult
import org.springframework.stereotype.Component

@Component
class ProductFacade(
    private val productCacheManager: ProductCacheManager,
) {

    fun getProducts(brandId: Long?, pageQuery: PageQuery): PageResult<ProductInfo> {
        return productCacheManager.getProducts(brandId, pageQuery)
    }

    fun getProduct(productId: Long): ProductDetailInfo {
        return productCacheManager.getProduct(productId)
    }
}
