package com.loopers.infrastructure.catalog.brand

import com.loopers.domain.PageResult
import com.loopers.domain.catalog.brand.entity.Brand
import com.loopers.domain.catalog.brand.repository.BrandRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface BrandJpaRepository : JpaRepository<BrandEntity, Long>

@Repository
class BrandRepositoryImpl(
    private val jpa: BrandJpaRepository,
) : BrandRepository {

    override fun save(brand: Brand): Brand {
        return jpa.save(BrandEntity.fromDomain(brand)).toDomain()
    }

    override fun findById(id: Long): Brand? {
        return jpa.findById(id).orElse(null)?.toDomain()
    }

    override fun findAll(page: Int, size: Int): PageResult<Brand> {
        val pageable = PageRequest.of(page, size)
        val result = jpa.findAll(pageable)
        return PageResult(result.content.map { it.toDomain() }, result.totalElements, page, size)
    }
}
