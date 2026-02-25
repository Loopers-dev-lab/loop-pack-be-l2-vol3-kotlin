package com.loopers.domain.brand

import com.loopers.domain.brand.vo.BrandName
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class BrandChanger(
    private val brandReader: BrandReader,
    private val brandRepository: BrandRepository,
) {

    fun changeName(id: Long, name: String): Brand {
        val brand = brandReader.getById(id)
        val newName = BrandName(name)

        if (brandRepository.existsByName(newName)) {
            throw CoreException(ErrorType.DUPLICATE_BRAND_NAME)
        }

        brand.changeName(newName)
        return brandRepository.save(brand)
    }
}
