package com.loopers.infrastructure.catalog

import com.loopers.domain.catalog.BrandModel
import com.loopers.domain.catalog.BrandRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Component

@Component
class BrandRepositoryImpl(
    private val brandJpaRepository: BrandJpaRepository,
) : BrandRepository {
    override fun findById(id: Long): BrandModel? {
        return brandJpaRepository.findByIdAndDeletedAtIsNull(id)
    }

    override fun findByName(name: String): BrandModel? {
        return brandJpaRepository.findByNameAndDeletedAtIsNull(name)
    }

    override fun findAll(pageable: Pageable): Slice<BrandModel> {
        return brandJpaRepository.findAllByDeletedAtIsNull(pageable)
    }

    override fun save(brand: BrandModel): BrandModel {
        return brandJpaRepository.save(brand)
    }
}
