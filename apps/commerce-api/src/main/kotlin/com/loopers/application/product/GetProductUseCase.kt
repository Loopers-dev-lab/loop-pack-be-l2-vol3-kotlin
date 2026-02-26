package com.loopers.application.product

import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ProductErrorCode
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetProductUseCase(
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
) {

    @Transactional(readOnly = true)
    fun execute(productId: Long): ProductInfo {
        val product = productRepository.findActiveByIdOrNull(productId)
            ?: throw CoreException(ProductErrorCode.PRODUCT_NOT_FOUND)

        val brand = brandRepository.findByIdOrNull(product.brandId)
        val brandName = brand?.name ?: ""

        return ProductInfo.from(product, brandName)
    }
}
