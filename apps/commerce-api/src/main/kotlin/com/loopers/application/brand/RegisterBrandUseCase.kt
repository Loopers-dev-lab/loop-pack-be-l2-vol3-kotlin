package com.loopers.application.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.support.error.BrandErrorCode
import com.loopers.support.error.CoreException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RegisterBrandUseCase(
    private val brandRepository: BrandRepository,
) {

    @Transactional
    fun execute(command: BrandCommand.Register): BrandInfo {
        if (brandRepository.existsActiveByName(command.name)) {
            throw CoreException(BrandErrorCode.DUPLICATE_BRAND_NAME)
        }
        val brand = Brand.create(command.name)
        val saved = brandRepository.save(brand)
        return BrandInfo.from(saved)
    }
}
