package com.loopers.application.catalog

import com.loopers.application.UseCase
import com.loopers.domain.catalog.BrandService
import com.loopers.domain.catalog.ProductService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserGetProductUseCase(
    private val productService: ProductService,
    private val brandService: BrandService,
) : UseCase<Long, UserGetProductResult> {
    @Transactional(readOnly = true)
    override fun execute(productId: Long): UserGetProductResult {
        val productInfo = productService.getProduct(productId)
        val brandInfo = brandService.findBrand(productInfo.brandId)
        return UserGetProductResult.from(productInfo, brandName = brandInfo?.name ?: "")
    }
}
