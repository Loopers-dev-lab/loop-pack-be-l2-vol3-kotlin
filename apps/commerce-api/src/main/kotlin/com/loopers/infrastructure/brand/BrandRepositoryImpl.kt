package com.loopers.infrastructure.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class BrandRepositoryImpl(
    private val brandJpaRepository: BrandJpaRepository,
) : BrandRepository {
    override fun save(brand: Brand): Brand {
        return brandJpaRepository.save(brand)
    }

    override fun findByIdAndDeletedAtIsNull(id: Long): Brand? {
        return brandJpaRepository.findByIdAndDeletedAtIsNull(id)
    }

    override fun findAllByDeletedAtIsNull(pageable: Pageable): Page<Brand> {
        return brandJpaRepository.findAllByDeletedAtIsNull(pageable)
    }
}
