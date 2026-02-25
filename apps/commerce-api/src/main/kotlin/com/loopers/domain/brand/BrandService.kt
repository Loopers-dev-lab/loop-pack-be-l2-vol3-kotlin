package com.loopers.domain.brand

import com.loopers.support.error.BrandException
import org.springframework.stereotype.Component

@Component
class BrandService(
    private val brandRepository: BrandRepository,
) {

    fun validateUniqueName(name: String) {
        if (brandRepository.existsActiveByName(name)) {
            throw BrandException.duplicateName()
        }
    }
}
