package com.loopers.domain.brand

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.transaction.annotation.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class BrandService(
    private val brandRepository: BrandRepository,
) {
    @Transactional(readOnly = true)
    fun getBrand(id: Long): Brand {
        return brandRepository.findByIdAndDeletedAtIsNull(id)
            ?: throw CoreException(ErrorType.NOT_FOUND)
    }

    @Transactional(readOnly = true)
    fun getBrands(pageable: Pageable): Page<Brand> {
        return brandRepository.findAllByDeletedAtIsNull(pageable)
    }

    @Transactional
    fun createBrand(name: String, description: String): Brand {
        return brandRepository.save(Brand(name = name, description = description))
    }

    @Transactional
    fun updateBrand(id: Long, name: String, description: String): Brand {
        val brand = brandRepository.findByIdAndDeletedAtIsNull(id)
            ?: throw CoreException(ErrorType.NOT_FOUND)

        brand.update(name = name, description = description)
        return brand
    }

    @Transactional
    fun deleteBrand(id: Long) {
        val brand = brandRepository.findByIdAndDeletedAtIsNull(id)
            ?: throw CoreException(ErrorType.NOT_FOUND)

        brand.delete()
    }
}
