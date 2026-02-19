package com.loopers.infrastructure.catalog.brand

import com.loopers.domain.PageResult
import com.loopers.domain.catalog.brand.entity.Brand
import com.loopers.domain.catalog.brand.repository.BrandRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class BrandRepositoryImpl(
    private val brandJpaRepository: BrandJpaRepository,
) : BrandRepository {

    override fun save(brand: Brand): Brand {
        return brandJpaRepository.save(brand)
    }

    override fun findById(id: Long): Brand? {
        return brandJpaRepository.findById(id).orElse(null)
    }

    override fun findAll(page: Int, size: Int): PageResult<Brand> {
        val pageable = PageRequest.of(page, size)
        val result = brandJpaRepository.findAll(pageable)
        return PageResult(result.content, result.totalElements, page, size)
    }
}
