package com.loopers.infrastructure.brand

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class BrandRepositoryImpl(
    private val brandJpaRepository: BrandJpaRepository,
) : BrandRepository {
    override fun save(brand: BrandModel): BrandModel {
        if (brand.id == 0L) {
            return brandJpaRepository.save(BrandJpaModel.from(brand)).toModel()
        }
        val existing = brandJpaRepository.findById(brand.id).orElseThrow()
        existing.updateFrom(brand)
        return existing.toModel()
    }

    override fun findById(id: Long): BrandModel? {
        return brandJpaRepository.findById(id).orElse(null)?.toModel()
    }

    override fun findAll(pageQuery: PageQuery): PageResult<BrandModel> {
        val pageable = PageRequest.of(pageQuery.page, pageQuery.size)
        val page = brandJpaRepository.findAll(pageable)
        return PageResult(
            content = page.content.map { it.toModel() },
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }
}
