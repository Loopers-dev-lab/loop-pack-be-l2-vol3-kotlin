package com.loopers.infrastructure.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class BrandRepositoryImpl(
    private val brandJpaRepository: BrandJpaRepository,
) : BrandRepository {
    override fun findById(id: Long): Brand? {
        return brandJpaRepository.findByIdOrNull(id)
            ?.takeIf { it.deletedAt == null }
    }

    override fun findAll(pageable: Pageable): Page<Brand> {
        return brandJpaRepository.findAllByDeletedAtIsNull(pageable)
    }

    override fun findAllByIds(ids: List<Long>): List<Brand> {
        return brandJpaRepository.findAllByIdInAndDeletedAtIsNull(ids)
    }

    override fun save(brand: Brand): Brand {
        return brandJpaRepository.save(brand)
    }
}
