package com.loopers.application.catalog.brand

import com.loopers.domain.PageResult
import com.loopers.domain.catalog.brand.repository.BrandRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetBrandsUseCase(private val brandRepository: BrandRepository) {
    @Transactional(readOnly = true)
    fun execute(page: Int, size: Int): PageResult<BrandInfo> {
        return brandRepository.findAll(page, size).map { BrandInfo.from(it) }
    }
}
