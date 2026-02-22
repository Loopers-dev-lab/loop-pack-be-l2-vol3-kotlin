package com.loopers.application.product

import com.loopers.domain.product.ProductService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class ProductFacade(
    private val productService: ProductService,
) {

    fun getProducts(brandId: Long?, pageable: Pageable): Page<ProductInfo> {
        return productService.getProducts(brandId, pageable)
            .map { ProductInfo.from(it) }
    }
}
