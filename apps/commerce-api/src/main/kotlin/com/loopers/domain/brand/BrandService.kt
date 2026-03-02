package com.loopers.domain.brand

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BrandService(
    private val brandRepository: BrandRepository,
) {

    @Transactional
    fun createBrand(command: CreateBrandCommand): Brand {
        val brand = Brand(
            name = command.name,
            description = command.description,
            imageUrl = command.imageUrl,
        )
        return brandRepository.save(brand)
    }

    @Transactional
    fun updateBrand(brandId: Long, command: UpdateBrandCommand): Brand {
        val brand = findById(brandId)
        brand.update(
            name = command.name,
            description = command.description,
            imageUrl = command.imageUrl,
        )
        return brand
    }

    @Transactional
    fun deleteBrand(brandId: Long) {
        val brand = findById(brandId)
        brand.delete()
    }

    @Transactional(readOnly = true)
    fun findById(brandId: Long): Brand {
        return brandRepository.findById(brandId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 브랜드입니다.")
    }

    @Transactional(readOnly = true)
    fun findByIds(ids: List<Long>): List<Brand> {
        return brandRepository.findByIds(ids)
    }

    @Transactional(readOnly = true)
    fun findAll(): List<Brand> {
        return brandRepository.findAll()
    }

    @Transactional(readOnly = true)
    fun findAll(pageable: Pageable): Page<Brand> {
        return brandRepository.findAll(pageable)
    }
}
