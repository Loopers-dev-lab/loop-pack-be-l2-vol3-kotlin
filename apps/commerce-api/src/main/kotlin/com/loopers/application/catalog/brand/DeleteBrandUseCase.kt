package com.loopers.application.catalog.brand

import com.loopers.domain.catalog.brand.repository.BrandRepository
import com.loopers.domain.catalog.product.repository.ProductRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DeleteBrandUseCase(
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
) {
    @Transactional
    fun execute(brandId: Long) {
        val brand = brandRepository.findById(brandId) ?: return
        if (brand.isDeleted()) return
        brand.delete()
        brandRepository.save(brand)

        val products = productRepository.findAllByBrandId(brandId)
        products.forEach { it.delete() }
        productRepository.saveAll(products)
    }
}
