package com.loopers.application.brand

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
        val brand = brandRepository.findActiveByIdOrNull(command.brandId)
            ?: throw CoreException(BrandErrorCode.BRAND_NOT_FOUND)
        if (brand.name != command.name) {
            if (brandRepository.existsActiveByName(command.name)) {
                throw CoreException(BrandErrorCode.DUPLICATE_BRAND_NAME)
            }
        }
        brand.update(command.name)
        return BrandInfo.from(brand)
    }
}
