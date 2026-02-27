package com.loopers.application.brand

import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.BrandErrorCode
import com.loopers.support.error.CoreException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DeleteBrandUseCase(
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
) {

    @Transactional
    fun execute(brandId: Long) {
        val brand = brandRepository.findActiveByIdOrNull(brandId)
            ?: throw CoreException(BrandErrorCode.BRAND_NOT_FOUND)
        brand.delete()

        val products = productRepository.findAllActiveByBrandId(brandId)
        products.forEach { it.delete() }
    }
}
