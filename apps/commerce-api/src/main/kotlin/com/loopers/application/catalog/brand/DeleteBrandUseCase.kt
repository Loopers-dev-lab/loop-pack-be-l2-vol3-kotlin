package com.loopers.application.catalog.brand

import com.loopers.domain.catalog.brand.repository.BrandRepository
import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.domain.common.vo.BrandId
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DeleteBrandUseCase(
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
) {
    @Transactional
    fun execute(brandId: Long) {
        val brand = brandRepository.findById(BrandId(brandId))
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
        if (brand.isDeleted()) {
            throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
        }
        brand.delete()
        brandRepository.save(brand)

        val products = productRepository.findAllByBrandId(BrandId(brandId))
        products.forEach { it.delete() }
        productRepository.saveAll(products)
    }
}
