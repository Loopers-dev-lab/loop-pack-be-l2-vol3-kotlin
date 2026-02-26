package com.loopers.infrastructure.catalog.brand

import com.loopers.domain.PageResult
import com.loopers.domain.catalog.brand.model.Brand
import com.loopers.domain.catalog.brand.repository.BrandRepository
import com.loopers.domain.common.vo.BrandId
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface BrandJpaRepository : JpaRepository<BrandEntity, Long> {
    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<BrandEntity>
}

@Repository
class BrandRepositoryImpl(
    private val brandJpaRepository: BrandJpaRepository,
) : BrandRepository {

    override fun save(brand: Brand): Brand {
        return brandJpaRepository.save(BrandEntity.fromDomain(brand)).toDomain()
    }

    override fun findById(id: BrandId): Brand? {
        return brandJpaRepository.findById(id.value).orElse(null)?.toDomain()
    }

    override fun findAll(page: Int, size: Int): PageResult<Brand> {
        val pageable = PageRequest.of(page, size)
        val result = brandJpaRepository.findAllByDeletedAtIsNull(pageable)
        return PageResult(result.content.map { it.toDomain() }, result.totalElements, page, size)
    }
}
