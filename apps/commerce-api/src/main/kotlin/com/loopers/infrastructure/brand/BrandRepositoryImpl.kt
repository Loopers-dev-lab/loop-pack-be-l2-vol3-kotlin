package com.loopers.infrastructure.brand

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.brand.BrandRepository
import org.springframework.stereotype.Component

@Component
class BrandRepositoryImpl(
    private val brandJpaRepository: BrandJpaRepository,
): BrandRepository {
    override fun findByName(name: String): BrandModel? {
        return brandJpaRepository.findByName(name)
    }

    override fun save(brand: BrandModel): BrandModel {
        return brandJpaRepository.save(brand)
    }
}
