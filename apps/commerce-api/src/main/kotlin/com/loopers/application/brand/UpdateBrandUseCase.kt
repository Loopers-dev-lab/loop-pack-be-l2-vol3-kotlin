package com.loopers.application.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.support.error.BrandErrorCode
import com.loopers.support.error.CoreException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UpdateBrandUseCase(
    private val brandRepository: BrandRepository,
) {

    @Transactional
    fun execute(command: BrandCommand.Update): BrandInfo {
        val brand = getActiveBrandOrThrow(command.brandId)
        if (brand.name != command.name) {
            if (brandRepository.existsActiveByName(command.name)) {
                throw CoreException(BrandErrorCode.DUPLICATE_BRAND_NAME)
            }
        }
        brand.update(command.name)
        val saved = brandRepository.save(brand)
        return BrandInfo.from(saved)
    }

    private fun getActiveBrandOrThrow(brandId: Long): Brand {
        val brand = brandRepository.findByIdOrNull(brandId)
            ?: throw CoreException(BrandErrorCode.BRAND_NOT_FOUND)
        if (brand.isDeleted()) throw CoreException(BrandErrorCode.BRAND_NOT_FOUND)
        return brand
    }
}
