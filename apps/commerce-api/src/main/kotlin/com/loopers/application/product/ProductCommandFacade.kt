package com.loopers.application.product

import com.loopers.domain.product.DisplayStatus
import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductService
import com.loopers.domain.product.SaleStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ProductCommandFacade(private val productService: ProductService, private val productCacheStore: ProductCacheStore) {
    @Transactional
    fun create(
        name: String,
        price: Long,
        brandId: Long,
        description: String?,
        thumbnailImageUrl: String?,
        stockQuantity: Int,
    ): ProductModel {
        val product = productService.create(
            name = name,
            price = price,
            brandId = brandId,
            description = description,
            thumbnailImageUrl = thumbnailImageUrl,
            stockQuantity = stockQuantity,
        )
        productCacheStore.evictProductList()
        return product
    }

    @Transactional
    fun update(
        id: Long,
        name: String,
        price: Long,
        description: String?,
        thumbnailImageUrl: String?,
        stockQuantity: Int,
        saleStatus: SaleStatus,
        displayStatus: DisplayStatus,
    ): ProductModel {
        val product = productService.update(
            id = id,
            name = name,
            price = price,
            description = description,
            thumbnailImageUrl = thumbnailImageUrl,
            stockQuantity = stockQuantity,
            saleStatus = saleStatus,
            displayStatus = displayStatus,
        )
        productCacheStore.evictProductDetail(product.id)
        productCacheStore.evictProductList()
        return product
    }

    @Transactional
    fun delete(id: Long) {
        productService.delete(id)
        productCacheStore.evictProductDetail(id)
        productCacheStore.evictProductList()
    }
}
