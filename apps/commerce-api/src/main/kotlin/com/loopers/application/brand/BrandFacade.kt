package com.loopers.application.brand

import com.loopers.application.product.ProductService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BrandFacade(
    private val brandService: BrandService,
    private val productService: ProductService,
) {

    @Transactional
    fun deleteBrand(brandId: Long) {
        brandService.deleteBrand(brandId)
        productService.deleteProductsByBrandId(brandId)
    }
}
