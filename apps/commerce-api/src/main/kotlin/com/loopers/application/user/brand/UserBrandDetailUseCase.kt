package com.loopers.application.user.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserBrandDetailUseCase(
    private val brandRepository: BrandRepository,
) {
    @Transactional(readOnly = true)
    fun getDetail(brandId: Long): UserBrandResult.Detail {
        val brand = brandRepository.findById(brandId)
            ?: throw CoreException(ErrorType.BRAND_NOT_FOUND)
        if (brand.status != Brand.Status.ACTIVE) {
            throw CoreException(ErrorType.BRAND_NOT_FOUND)
        }
        return UserBrandResult.Detail.from(brand)
    }
}
