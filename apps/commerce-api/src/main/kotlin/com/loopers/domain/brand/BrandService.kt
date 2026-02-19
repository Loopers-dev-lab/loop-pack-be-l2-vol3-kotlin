package com.loopers.domain.brand

import com.loopers.domain.brand.dto.BrandInfo
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class BrandService(
    private val brandRepository: BrandRepository,
) {

    fun getBrandInfo(id: Long): BrandInfo {
        val brand = findBrand(id)
        return BrandInfo.from(brand)
    }

    fun getAllBrands(pageable: Pageable): Page<BrandInfo> {
        val brandPage = brandRepository.findAll(pageable)
        val brandInfos = brandPage.map { BrandInfo.from(it) }
        return brandInfos
    }

    @Transactional
    fun createBrand(name: String, description: String): Brand {
        if (brandRepository.existsByName(name)) {
            throw CoreException(ErrorType.BAD_REQUEST, "이미 존재하는 브랜드명 입니다.")
        }
        val brand = Brand.create(name = name, description = description)
        return brandRepository.save(brand)
    }

    @Transactional
    fun updateBrand(id: Long, name: String, description: String) {
        val brand = findBrand(id)
        brand.updateInfo(name, description)
    }

    @Transactional
    fun deleteBrand(id: Long) {
        val brand = findBrand(id)
        brand.delete()
    }

    fun getBrand(brandId: Long): Brand {
        return brandRepository.findById(brandId)
    }

    private fun findBrand(id: Long) = (
        brandRepository.findById(id)
            .takeIf { !it.isDeleted() }
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드가 존재하지 않습니다.")
    )
}
