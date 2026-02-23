package com.loopers.domain.brand

import com.loopers.support.error.BrandException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BrandService(
    private val brandRepository: BrandRepository,
) {

    @Transactional
    fun register(name: String): Brand {
        if (brandRepository.existsActiveByName(name)) {
            throw BrandException.duplicateName()
        }
        val brand = Brand.create(name)
        return brandRepository.save(brand)
    }

    @Transactional
    fun update(brandId: Long, name: String): Brand {
        val brand = getActiveBrand(brandId)
        if (brand.name != name && brandRepository.existsActiveByName(name)) {
            throw BrandException.duplicateName()
        }
        brand.update(name)
        return brandRepository.save(brand)
    }

    @Transactional
    fun delete(brandId: Long) {
        val brand = getActiveBrand(brandId)
        brand.delete()
        brandRepository.save(brand)
    }

    @Transactional(readOnly = true)
    fun getActiveBrand(brandId: Long): Brand {
        val brand = brandRepository.findById(brandId)
            ?: throw BrandException.notFound()
        if (brand.isDeleted()) throw BrandException.notFound()
        return brand
    }
}
