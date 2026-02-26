package com.loopers.domain.brand

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class BrandService(
    private val brandRepository: BrandRepository,
) {

    fun createBrand(name: String, description: String?): Brand {
        return brandRepository.save(Brand(name = name, description = description))
    }

    fun getBrands(pageable: Pageable): Page<Brand> {
        return brandRepository.findAll(pageable)
    }

    fun getBrand(brandId: Long): Brand {
        return brandRepository.findById(brandId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
    }

    fun getBrandsByIds(ids: List<Long>): List<Brand> {
        return brandRepository.findAllByIds(ids)
    }

    fun updateBrand(brandId: Long, name: String, description: String?): Brand {
        val brand = getBrand(brandId)
        brand.update(name, description)
        return brandRepository.save(brand)
    }
}
