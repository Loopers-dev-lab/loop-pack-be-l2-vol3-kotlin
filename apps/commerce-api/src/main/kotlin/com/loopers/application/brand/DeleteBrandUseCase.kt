package com.loopers.application.brand

import com.loopers.domain.brand.Brand
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
        val brand = getActiveBrandOrThrow(brandId)
        brand.delete()
        brandRepository.save(brand)

        val products = productRepository.findAllActiveByBrandId(brandId)
        products.forEach { it.delete() }
        if (products.isNotEmpty()) {
            productRepository.saveAll(products)
        }
    }

    private fun getActiveBrandOrThrow(brandId: Long): Brand {
        val brand = brandRepository.findByIdOrNull(brandId)
            ?: throw CoreException(BrandErrorCode.BRAND_NOT_FOUND)
        if (brand.isDeleted()) throw CoreException(BrandErrorCode.BRAND_NOT_FOUND)
        return brand
    }
}
