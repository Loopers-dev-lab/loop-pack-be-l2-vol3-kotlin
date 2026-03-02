package com.loopers.infrastructure.brand

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.brand.BusinessNumber
import com.loopers.domain.brand.Name
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class BrandRepositoryImpl(
    private val brandJpaRepository: BrandJpaRepository,
) : BrandRepository {

    override fun save(brand: BrandModel): BrandModel {
        return brandJpaRepository.save(brand)
    }

    override fun findById(id: Long): BrandModel? {
        return brandJpaRepository.findById(id).orElse(null)
    }

    override fun findAll(pageable: Pageable): Page<BrandModel> {
        return brandJpaRepository.findAll(pageable)
    }

    override fun existsByName(name: Name): Boolean {
        return brandJpaRepository.existsByName(name)
    }

    override fun existsByBusinessNumber(businessNumber: BusinessNumber): Boolean {
        return brandJpaRepository.existsByBusinessNumber(businessNumber)
    }
}
