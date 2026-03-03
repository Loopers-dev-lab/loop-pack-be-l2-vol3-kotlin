package com.loopers.application.catalog

import com.loopers.application.UseCase
import com.loopers.domain.catalog.BrandService
import com.loopers.domain.catalog.ProductService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminGetProductUseCase(
    private val productService: ProductService,
    private val brandService: BrandService,
) : UseCase<Long, GetProductResult> {

    @Transactional(readOnly = true)
    override fun execute(productId: Long): GetProductResult {
        val productInfo = productService.getProduct(productId)
        val brandInfo = brandService.findBrand(productInfo.brandId)
        return GetProductResult.from(productInfo, brandName = brandInfo?.name ?: "")
    }
}
