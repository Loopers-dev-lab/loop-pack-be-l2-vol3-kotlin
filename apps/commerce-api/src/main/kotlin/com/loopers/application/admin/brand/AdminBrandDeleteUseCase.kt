package com.loopers.application.admin.brand

import com.loopers.domain.brand.BrandRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminBrandDeleteUseCase(
    private val brandRepository: BrandRepository,
) {
    @Transactional
    fun delete(brandId: Long, admin: String) {
        brandRepository.findById(brandId)
            ?: throw CoreException(ErrorType.BRAND_NOT_FOUND)
        // TODO: Product 도메인 구현 시 해당 브랜드의 모든 상품도 함께 삭제해야 함
        //       (요구사항: "브랜드 제거 시, 해당 브랜드의 상품들도 삭제되어야 함")
        brandRepository.delete(brandId, admin)
    }
}
