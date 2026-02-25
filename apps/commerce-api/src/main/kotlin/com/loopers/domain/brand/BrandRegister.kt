package com.loopers.domain.brand

import com.loopers.domain.brand.vo.BrandName
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class BrandRegister(
    private val brandRepository: BrandRepository,
) {

    fun register(name: String): Brand {
        val brandName = BrandName(name)

        if (brandRepository.existsByName(brandName)) {
            throw CoreException(ErrorType.DUPLICATE_BRAND_NAME)
        }

        val brand = Brand(name = brandName)
        return brandRepository.save(brand)
    }
}
