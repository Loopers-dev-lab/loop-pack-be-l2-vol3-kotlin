package com.loopers.application.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandDomainService
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BrandService(
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
    private val brandDomainService: BrandDomainService,
) {

    @Transactional(readOnly = true)
    fun getBrand(brandId: Long): Brand {
        return brandRepository.findById(brandId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
    }

    @Transactional(readOnly = true)
    fun validateBrandExists(brandId: Long) {
        if (!brandRepository.existsById(brandId)) {
            throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
        }
    }

    @Transactional(readOnly = true)
    fun getBrandInfo(brandId: Long): BrandInfo {
        return BrandInfo.from(getBrand(brandId))
    }

    @Transactional(readOnly = true)
    fun getBrandIncludingDeleted(brandId: Long): Brand? {
        return brandRepository.findByIdIncludingDeleted(brandId)
    }

    @Transactional(readOnly = true)
    fun getAllBrands(pageable: Pageable): Page<BrandInfo> {
        return brandRepository.findAll(pageable).map { BrandInfo.from(it) }
    }

    @Transactional
    fun createBrand(criteria: CreateBrandCriteria): BrandInfo {
        if (brandRepository.existsByName(criteria.name)) {
            throw CoreException(ErrorType.CONFLICT, "이미 존재하는 브랜드명입니다.")
        }
        val brand = brandRepository.save(Brand(name = criteria.name, description = criteria.description))
        return BrandInfo.from(brand)
    }

    @Transactional
    fun updateBrand(brandId: Long, criteria: UpdateBrandCriteria): BrandInfo {
        val brand = brandRepository.findById(brandId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
        if (brandRepository.existsByNameAndIdNot(criteria.name, brandId)) {
            throw CoreException(ErrorType.CONFLICT, "이미 존재하는 브랜드명입니다.")
        }
        brand.update(name = criteria.name, description = criteria.description)
        val savedBrand = brandRepository.save(brand)
        return BrandInfo.from(savedBrand)
    }

    @Transactional
    fun deleteBrand(brandId: Long) {
        val brand = brandRepository.findById(brandId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
        val products = productRepository.findAllByBrandId(brandId)

        brandDomainService.deleteBrand(brand, products)

        brandRepository.save(brand)
        products.forEach { productRepository.save(it) }
    }
}
