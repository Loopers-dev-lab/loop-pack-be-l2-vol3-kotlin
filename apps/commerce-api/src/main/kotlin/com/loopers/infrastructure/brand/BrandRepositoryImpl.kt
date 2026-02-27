package com.loopers.infrastructure.brand

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.brand.BrandRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class BrandRepositoryImpl(
    private val brandJpaRepository: BrandJpaRepository,
) : BrandRepository {

    override fun save(brand: BrandModel): BrandModel {
        return brandJpaRepository.save(brand)
    }

    override fun findByIdAndDeletedAtIsNull(id: Long): BrandModel? {
        return brandJpaRepository.findByIdAndDeletedAtIsNull(id)
    }

    override fun findAllByDeletedAtIsNull(pageable: Pageable): Page<BrandModel> {
        return brandJpaRepository.findAllByDeletedAtIsNull(pageable)
    }

    override fun existsByNameAndDeletedAtIsNull(name: String): Boolean {
        return brandJpaRepository.existsByNameAndDeletedAtIsNull(name)
    }

    override fun findAllByIdInAndDeletedAtIsNull(ids: List<Long>): List<BrandModel> {
        return brandJpaRepository.findAllByIdInAndDeletedAtIsNull(ids)
    }
}
