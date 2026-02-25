package com.loopers.infrastructure.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.brand.BrandStatus
import com.loopers.domain.brand.vo.BrandName
import org.springframework.stereotype.Component

@Component
class BrandRepositoryImpl(
    private val brandJpaRepository: BrandJpaRepository,
    private val brandMapper: BrandMapper,
) : BrandRepository {

    override fun save(brand: Brand): Brand {
        val entity = resolveEntity(brand)
        val savedEntity = brandJpaRepository.save(entity)
        return brandMapper.toDomain(savedEntity)
    }

    override fun findById(id: Long): Brand? {
        return brandJpaRepository.findById(id)
            .map { brandMapper.toDomain(it) }
            .orElse(null)
    }

    override fun findByName(name: BrandName): Brand? {
        return brandJpaRepository.findByName(name.value)?.let { brandMapper.toDomain(it) }
    }

    override fun existsByName(name: BrandName): Boolean {
        return brandJpaRepository.existsByName(name.value)
    }

    override fun findAllByStatus(status: BrandStatus): List<Brand> {
        return brandJpaRepository.findAllByStatus(status.name).map { brandMapper.toDomain(it) }
    }

    override fun findAllByIds(ids: List<Long>): List<Brand> {
        return brandJpaRepository.findAllByIdIn(ids).map { brandMapper.toDomain(it) }
    }

    private fun resolveEntity(brand: Brand): BrandEntity {
        if (brand.id == null) return brandMapper.toEntity(brand)

        val entity = brandJpaRepository.getReferenceById(brand.id)
        brandMapper.update(entity, brand)
        return entity
    }
}
