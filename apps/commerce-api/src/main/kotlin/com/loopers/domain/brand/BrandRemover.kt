package com.loopers.domain.brand

import org.springframework.stereotype.Component

@Component
class BrandRemover(
    private val brandReader: BrandReader,
    private val brandRepository: BrandRepository,
) {

    fun remove(id: Long) {
        val brand = brandReader.getById(id)
        brand.deactivate()
        brandRepository.save(brand)
    }
}
