package com.loopers.application.catalog

import com.loopers.application.UseCase
import com.loopers.domain.catalog.BrandInfo
import com.loopers.domain.catalog.BrandRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserGetBrandUseCase(
    private val brandRepository: BrandRepository,
) : UseCase<Long, UserGetBrandResult> {
    @Transactional(readOnly = true)
    override fun execute(brandId: Long): UserGetBrandResult {
        val brand = brandRepository.findById(brandId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
        val info = BrandInfo.from(brand)
        return UserGetBrandResult.from(info)
    }
}
