package com.loopers.application.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.brand.BrandService
import com.loopers.support.error.BrandException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BrandFacade(
    private val brandService: BrandService,
    private val brandRepository: BrandRepository,
) {

    @Transactional
    fun register(command: BrandCommand.Register): BrandInfo {
        brandService.validateUniqueName(command.name)
        val brand = Brand.create(command.name)
        val saved = brandRepository.save(brand)
        return BrandInfo.from(saved)
    }

    @Transactional
    fun update(command: BrandCommand.Update): BrandInfo {
        val brand = getActiveBrandOrThrow(command.brandId)
        if (brand.name != command.name) {
            brandService.validateUniqueName(command.name)
        }
        brand.update(command.name)
        val saved = brandRepository.save(brand)
        return BrandInfo.from(saved)
    }

    @Transactional
    fun delete(brandId: Long) {
        val brand = getActiveBrandOrThrow(brandId)
        brand.delete()
        brandRepository.save(brand)
    }

    @Transactional(readOnly = true)
    fun getActiveBrand(brandId: Long): BrandInfo {
        val brand = getActiveBrandOrThrow(brandId)
        return BrandInfo.from(brand)
    }

    @Transactional(readOnly = true)
    fun getAllActiveBrands(): List<BrandInfo> {
        return brandRepository.findAllActive().map { BrandInfo.from(it) }
    }

    private fun getActiveBrandOrThrow(brandId: Long): Brand {
        val brand = brandRepository.findByIdOrNull(brandId)
            ?: throw BrandException.notFound()
        if (brand.isDeleted()) throw BrandException.notFound()
        return brand
    }
}
