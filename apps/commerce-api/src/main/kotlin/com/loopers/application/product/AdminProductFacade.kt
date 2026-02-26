package com.loopers.application.product

import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductService
import org.springframework.stereotype.Component

@Component
class AdminProductFacade(
    private val productService: ProductService,
    private val brandService: BrandService,
) {
    fun createProduct(
        name: String,
        description: String?,
        price: Long,
        stockQuantity: Int,
        brandId: Long,
    ): ProductInfo {
        brandService.getBrand(brandId)
        return productService.createProduct(
            name = name,
            description = description,
            price = price,
            stockQuantity = stockQuantity,
            brandId = brandId,
        ).let { ProductInfo.from(it) }
    }
}
