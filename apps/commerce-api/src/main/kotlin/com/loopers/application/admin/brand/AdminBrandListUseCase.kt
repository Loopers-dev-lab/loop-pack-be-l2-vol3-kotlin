package com.loopers.application.admin.brand

import com.loopers.domain.brand.BrandRepository
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminBrandListUseCase(
    private val brandRepository: BrandRepository,
) {
    @Transactional(readOnly = true)
    fun getList(pageRequest: PageRequest): PageResponse<AdminBrandResult.Summary> {
        return brandRepository.findAll(pageRequest)
            .map { AdminBrandResult.Summary.from(it) }
    }
}
