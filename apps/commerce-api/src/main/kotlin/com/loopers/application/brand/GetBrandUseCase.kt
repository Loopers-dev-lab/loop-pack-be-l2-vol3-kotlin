package com.loopers.application.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.support.error.BrandException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetBrandUseCase(
    private val brandRepository: BrandRepository,
) {

    @Transactional(readOnly = true)
    fun execute(brandId: Long): BrandInfo {
        val brand = getActiveBrandOrThrow(brandId)
        return BrandInfo.from(brand)
    }

    private fun getActiveBrandOrThrow(brandId: Long): Brand {
        val brand = brandRepository.findByIdOrNull(brandId)
            ?: throw BrandException.notFound()
        if (brand.isDeleted()) throw BrandException.notFound()
        return brand
    }
}
