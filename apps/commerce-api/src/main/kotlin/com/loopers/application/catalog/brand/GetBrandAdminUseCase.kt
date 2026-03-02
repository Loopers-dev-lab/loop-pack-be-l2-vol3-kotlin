package com.loopers.application.catalog.brand

import com.loopers.domain.catalog.brand.repository.BrandRepository
import com.loopers.domain.common.vo.BrandId
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetBrandAdminUseCase(private val brandRepository: BrandRepository) {
    @Transactional(readOnly = true)
    fun execute(brandId: Long): BrandInfo {
        val brand = brandRepository.findById(BrandId(brandId))
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
        return BrandInfo.from(brand)
    }
}
