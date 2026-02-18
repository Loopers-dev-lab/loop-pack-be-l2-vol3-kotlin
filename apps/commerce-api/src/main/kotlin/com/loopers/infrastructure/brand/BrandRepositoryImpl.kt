package com.loopers.infrastructure.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class BrandRepositoryImpl(
    private val brandJpaRepository: BrandJpaRepository,
) : BrandRepository {
    override fun findById(id: Long): Brand = brandJpaRepository.findById(id)
        .orElseThrow { CoreException(ErrorType.NOT_FOUND) }

    override fun findAll(pageable: Pageable): Page<Brand> =
        brandJpaRepository.findAll(pageable)

    override fun existsByName(name: String): Boolean =
        brandJpaRepository.existsByName(name)

    override fun save(brand: Brand): Brand = brandJpaRepository.save(brand)
}
