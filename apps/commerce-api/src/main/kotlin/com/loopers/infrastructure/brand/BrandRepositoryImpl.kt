package com.loopers.infrastructure.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.springframework.data.domain.PageRequest as SpringPageRequest
import org.springframework.stereotype.Repository

@Repository
class BrandRepositoryImpl(
    private val brandJpaRepository: BrandJpaRepository,
    private val brandMapper: BrandMapper,
) : BrandRepository {
    override fun save(brand: Brand, admin: String): Brand {
        val entity = if (brand.id != null) {
            val existing = brandJpaRepository.findById(brand.id).orElseThrow()
            existing.name = brand.name.value
            existing.status = brand.status
            existing.updateBy(admin)
            existing
        } else {
            brandMapper.toEntity(brand, admin)
        }
        return brandMapper.toDomain(brandJpaRepository.saveAndFlush(entity))
    }

    override fun delete(brandId: Long, admin: String) {
        val entity = brandJpaRepository.findById(brandId).orElseThrow()
        entity.deleteBy(admin)
        brandJpaRepository.saveAndFlush(entity)
    }

    override fun findById(id: Long): Brand? {
        return brandJpaRepository.findByIdAndDeletedAtIsNull(id)?.let { brandMapper.toDomain(it) }
    }

    override fun findAll(): List<Brand> {
        return brandJpaRepository.findAllByDeletedAtIsNull().map { brandMapper.toDomain(it) }
    }

    override fun findAllByIdIn(ids: List<Long>): List<Brand> {
        if (ids.isEmpty()) return emptyList()
        return brandJpaRepository.findAllByIdInAndDeletedAtIsNull(ids).map { brandMapper.toDomain(it) }
    }

    override fun findAll(pageRequest: PageRequest): PageResponse<Brand> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = brandJpaRepository.findAllByDeletedAtIsNull(pageable)
        return PageResponse(
            content = page.content.map { brandMapper.toDomain(it) },
            totalElements = page.totalElements,
            page = pageRequest.page,
            size = pageRequest.size,
        )
    }
}
