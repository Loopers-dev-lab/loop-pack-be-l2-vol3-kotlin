package com.loopers.domain.brand

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class BrandReader(
    private val brandRepository: BrandRepository,
) {

    fun getById(id: Long): Brand {
        return brandRepository.findById(id)
            ?: throw CoreException(ErrorType.BRAND_NOT_FOUND)
    }

    fun getActiveById(id: Long): Brand {
        val brand = getById(id)
        if (brand.status != BrandStatus.ACTIVE) {
            throw CoreException(ErrorType.BRAND_NOT_ACTIVE)
        }
        return brand
    }

    fun getAllActive(): List<Brand> {
        return brandRepository.findAllByStatus(BrandStatus.ACTIVE)
    }

    fun getAllByIds(ids: List<Long>): List<Brand> {
        return brandRepository.findAllByIds(ids)
    }
}
