package com.loopers.infrastructure.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult
import com.loopers.infrastructure.common.toPageRequest
import com.loopers.infrastructure.common.toPageResult
import org.springframework.stereotype.Component

@Component
class BrandRepositoryImpl(
    private val brandJpaRepository: BrandJpaRepository,
) : BrandRepository {
    override fun findById(id: Long): Brand? {
        return brandJpaRepository.findByIdAndDeletedAtIsNull(id)
    }

    override fun findAll(pageQuery: PageQuery): PageResult<Brand> {
        return brandJpaRepository.findAllByDeletedAtIsNull(pageQuery.toPageRequest())
            .toPageResult()
    }

    override fun findAllByIds(ids: List<Long>): List<Brand> {
        return brandJpaRepository.findAllByIdInAndDeletedAtIsNull(ids)
    }

    override fun save(brand: Brand): Brand {
        return brandJpaRepository.save(brand)
    }
}
