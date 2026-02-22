package com.loopers.infrastructure.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.support.error.BrandException
import org.springframework.stereotype.Repository

@Repository
class BrandRepositoryImpl(
    private val brandJpaRepository: BrandJpaRepository,
) : BrandRepository {

    override fun findById(id: Long): Brand? {
        return brandJpaRepository.findById(id)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findAllActive(): List<Brand> {
        return brandJpaRepository.findAllByDeletedAtIsNull()
            .map { it.toDomain() }
    }

    override fun findAll(): List<Brand> {
        return brandJpaRepository.findAll()
            .map { it.toDomain() }
    }

    override fun existsActiveByName(name: String): Boolean {
        return brandJpaRepository.existsByNameAndDeletedAtIsNull(name)
    }

    override fun save(brand: Brand): Brand {
        val entity = if (brand.id == 0L) {
            BrandEntity.fromDomain(brand)
        } else {
            brandJpaRepository.findById(brand.id)
                .orElseThrow { BrandException.notFound() }
                .apply { updateFromDomain(brand) }
        }
        return brandJpaRepository.save(entity).toDomain()
    }
}
