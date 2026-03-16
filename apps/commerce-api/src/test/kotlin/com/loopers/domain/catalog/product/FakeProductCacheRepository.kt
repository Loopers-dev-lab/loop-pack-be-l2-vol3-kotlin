package com.loopers.domain.catalog.product

import com.loopers.domain.catalog.product.model.Product
import com.loopers.domain.catalog.product.repository.ProductCacheRepository
import com.loopers.domain.common.vo.BrandId
import com.loopers.domain.common.vo.ProductId

class FakeProductCacheRepository : ProductCacheRepository {

    private val detailCache = mutableMapOf<ProductId, Product>()
    val evictedListBrandIds = mutableListOf<BrandId?>()

    override fun findProductDetail(productId: ProductId): Product? {
        return detailCache[productId]?.deepCopy()
    }

    override fun saveProductDetail(product: Product) {
        detailCache[product.id] = product.deepCopy()
    }

    override fun saveProductDetailIfAbsent(product: Product) {
        detailCache.putIfAbsent(product.id, product.deepCopy())
    }

    override fun evictProductDetail(productId: ProductId) {
        detailCache.remove(productId)
    }

    override fun evictProductList(brandId: BrandId?) {
        evictedListBrandIds.add(brandId)
    }

    private fun Product.deepCopy(): Product = Product(
        id = id,
        refBrandId = refBrandId,
        name = name,
        price = price,
        stock = stock,
        status = status,
        likeCount = likeCount,
        deletedAt = deletedAt,
    )
}
