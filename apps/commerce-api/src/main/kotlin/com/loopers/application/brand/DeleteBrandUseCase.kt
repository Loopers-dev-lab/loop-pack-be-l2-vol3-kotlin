package com.loopers.application.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.support.error.BrandException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DeleteBrandUseCase(
    private val brandRepository: BrandRepository,
) {

    @Transactional
    fun execute(brandId: Long) {
        val brand = getActiveBrandOrThrow(brandId)
        brand.delete()
        brandRepository.save(brand)
    }

    private fun getActiveBrandOrThrow(brandId: Long): Brand {
        val brand = brandRepository.findByIdOrNull(brandId)
            ?: throw BrandException.notFound()
        if (brand.isDeleted()) throw BrandException.notFound()
        return brand
    }
}
