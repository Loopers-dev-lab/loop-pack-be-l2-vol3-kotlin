package com.loopers.application.brand

import com.loopers.domain.brand.BrandName
import com.loopers.domain.brand.BrandRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UpdateBrandUseCase(
    private val brandRepository: BrandRepository,
) {
    @Transactional
    fun update(id: Long, command: UpdateBrandCommand): BrandInfo {
        val brand = brandRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다: $id")

        val updatedBrand = brand.update(
            name = BrandName(command.name),
            description = command.description,
            logoUrl = command.logoUrl,
        )

        brandRepository.save(updatedBrand)
        return BrandInfo.from(updatedBrand)
    }
}
