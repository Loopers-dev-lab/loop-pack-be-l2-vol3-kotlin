package com.loopers.infrastructure.catalog.brand

import com.loopers.domain.catalog.brand.Brand
import com.loopers.domain.catalog.brand.BrandRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
class BrandRepositoryImpl(
    private val brandJpaRepository: BrandJpaRepository,
) : BrandRepository {

    override fun save(brand: Brand): Brand {
        val entity = if (brand.id > 0L) {
            brandJpaRepository.getReferenceById(brand.id).apply {
                update(brand.name, brand.description)
            }
        } else {
            BrandEntity.from(brand)
        }
        return brandJpaRepository.save(entity).toDomain()
    }

    override fun findById(id: Long): Brand? =
        brandJpaRepository.findById(id)
            .filter { it.deletedAt == null }
            .map { it.toDomain() }
            .orElse(null)

    override fun findAll(page: Int, size: Int): List<Brand> =
        brandJpaRepository.findAllActive(PageRequest.of(page, size))
            .map { it.toDomain() }

    override fun deleteById(id: Long) {
        brandJpaRepository.findById(id)
            .filter { it.deletedAt == null }
            .ifPresent { it.delete() }
    }

    override fun existsById(id: Long): Boolean =
        brandJpaRepository.findById(id)
            .filter { it.deletedAt == null }
            .isPresent
}
