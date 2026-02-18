package com.loopers.application.brand

import com.loopers.domain.brand.BrandRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetBrandUseCase(
    private val brandRepository: BrandRepository,
) {
    fun getById(id: Long): BrandInfo {
        val brand = brandRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다: $id")
        return BrandInfo.from(brand)
    }

    fun getActiveById(id: Long): BrandInfo {
        val brand = brandRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다: $id")
        if (brand.isDeleted()) {
            throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다: $id")
        }
        return BrandInfo.from(brand)
    }
}
