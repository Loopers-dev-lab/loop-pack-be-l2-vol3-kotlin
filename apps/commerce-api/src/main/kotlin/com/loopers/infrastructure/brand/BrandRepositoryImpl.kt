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

    override fun findById(id: Long): Brand? {
        return brandJpaRepository.findByIdAndDeletedAtIsNull(id)
    }

    override fun findByIdIncludingDeleted(id: Long): Brand? {
        return brandJpaRepository.findById(id).orElse(null)
    }

    override fun findAll(pageable: Pageable): Page<Brand> {
        return brandJpaRepository.findAllByDeletedAtIsNull(pageable)
    }

    override fun existsByName(name: String): Boolean {
        return brandJpaRepository.existsByNameAndDeletedAtIsNull(name)
    }

    override fun existsByNameAndIdNot(name: String, id: Long): Boolean {
        return brandJpaRepository.existsByNameAndDeletedAtIsNullAndIdNot(name, id)
    }
}
