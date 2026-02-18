package com.loopers.infrastructure.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandName
import com.loopers.domain.brand.BrandRepository
import org.springframework.stereotype.Repository

@Repository
class BrandRepositoryImpl(
    private val jpaRepository: BrandJpaRepository,
) : BrandRepository {

    override fun save(brand: Brand): Long {
        val entity = BrandMapper.toEntity(brand)
        val saved = jpaRepository.save(entity)
        return requireNotNull(saved.id) {
            "Brand 저장 실패: id가 생성되지 않았습니다."
        }
    }

    override fun findById(id: Long): Brand? {
        return jpaRepository.findById(id).orElse(null)?.let { BrandMapper.toDomain(it) }
    }

    override fun existsByName(name: BrandName): Boolean {
        return jpaRepository.existsByName(name.value)
    }

    override fun findAll(): List<Brand> {
        return jpaRepository.findAll().map { BrandMapper.toDomain(it) }
    }

    override fun findAllActive(): List<Brand> {
        return jpaRepository.findAllActive().map { BrandMapper.toDomain(it) }
    }
}
