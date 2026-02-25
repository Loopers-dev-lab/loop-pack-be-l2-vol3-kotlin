package com.loopers.application.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.brand.CreateBrandCommand
import com.loopers.domain.brand.UpdateBrandCommand
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

    @Transactional(readOnly = true)
    fun getBrand(brandId: Long): Brand {
        return brandRepository.findById(brandId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
    }

    @Transactional(readOnly = true)
    fun getAllBrands(pageable: Pageable): Page<Brand> {
        return brandRepository.findAll(pageable)
    }

    @Transactional
    fun createBrand(command: CreateBrandCommand): Brand {
        if (brandRepository.existsByName(command.name)) {
            throw CoreException(ErrorType.CONFLICT, "이미 존재하는 브랜드명입니다.")
        }
        return brandRepository.save(Brand(name = command.name, description = command.description))
    }

    @Transactional
    fun updateBrand(brandId: Long, command: UpdateBrandCommand): Brand {
        val brand = brandRepository.findById(brandId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
        if (brandRepository.existsByNameAndIdNot(command.name, brandId)) {
            throw CoreException(ErrorType.CONFLICT, "이미 존재하는 브랜드명입니다.")
        }
        brand.update(name = command.name, description = command.description)
        return brandRepository.save(brand)
    }

    @Transactional
    fun deleteBrand(brandId: Long) {
        val brand = brandRepository.findById(brandId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
        brand.delete()
        brandRepository.save(brand)
    }
}
