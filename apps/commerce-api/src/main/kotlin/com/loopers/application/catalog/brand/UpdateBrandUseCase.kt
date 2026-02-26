package com.loopers.application.catalog.brand

import com.loopers.domain.catalog.brand.repository.BrandRepository
import com.loopers.domain.catalog.brand.vo.BrandName
import com.loopers.domain.common.vo.BrandId
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UpdateBrandUseCase(private val brandRepository: BrandRepository) {
    @Transactional
    fun execute(brandId: Long, name: String): BrandInfo {
        val brand = brandRepository.findById(BrandId(brandId))
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
        if (brand.isDeleted()) {
            throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
        }
        brand.update(BrandName(name))
        val saved = brandRepository.save(brand)
        return BrandInfo.from(saved)
    }
}
