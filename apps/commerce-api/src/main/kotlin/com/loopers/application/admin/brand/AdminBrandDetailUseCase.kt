package com.loopers.application.admin.brand

import com.loopers.domain.brand.BrandRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminBrandDetailUseCase(
    private val brandRepository: BrandRepository,
) {
    @Transactional(readOnly = true)
    fun getDetail(brandId: Long): AdminBrandResult.Detail {
        val brand = brandRepository.findById(brandId) ?: throw CoreException(ErrorType.BRAND_NOT_FOUND)
        return AdminBrandResult.Detail.from(brand)
    }
}
