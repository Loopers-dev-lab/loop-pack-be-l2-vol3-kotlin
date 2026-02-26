package com.loopers.application.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.support.error.BrandErrorCode
import com.loopers.support.error.CoreException
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
            ?: throw CoreException(BrandErrorCode.BRAND_NOT_FOUND)
        if (brand.isDeleted()) throw CoreException(BrandErrorCode.BRAND_NOT_FOUND)
        return brand
    }
}
