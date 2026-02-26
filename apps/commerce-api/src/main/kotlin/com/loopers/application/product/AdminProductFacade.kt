package com.loopers.application.product

import com.loopers.domain.brand.BrandService
import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult
import com.loopers.domain.product.ProductService
import org.springframework.stereotype.Component

@Component
class AdminProductFacade(
    private val productService: ProductService,
    private val brandService: BrandService,
) {
    fun getProducts(brandId: Long?, pageQuery: PageQuery): PageResult<AdminProductInfo> {
        val productPage = productService.getProducts(brandId, pageQuery)
        val brandIds = productPage.content.map { it.brandId }.distinct()
        val brandMap = if (brandIds.isNotEmpty()) {
            brandService.getBrandsByIds(brandIds).associateBy { it.id }
        } else {
            emptyMap()
        }
        return productPage.map { product ->
            AdminProductInfo.from(product, brandMap.getValue(product.brandId))
        }
    }

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
