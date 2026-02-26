package com.loopers.application.catalog

import com.loopers.application.UseCase
import com.loopers.domain.catalog.BrandRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminUpdateBrandUseCase(
    private val brandRepository: BrandRepository,
) : UseCase<UpdateBrandCriteria, Unit> {

    @Transactional
    override fun execute(criteria: UpdateBrandCriteria) {
        val brand = brandRepository.findById(criteria.brandId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")

        criteria.newName
            ?.takeIf { it != brand.name }
            ?.let { name ->
                brandRepository.findByName(name)?.let {
                    throw CoreException(ErrorType.CONFLICT, "이미 존재하는 브랜드입니다.")
                }
            }

        brand.update(
            newName = criteria.newName,
            newDescription = criteria.newDescription,
            newLogoUrl = criteria.newLogoUrl,
        )
    }
}
