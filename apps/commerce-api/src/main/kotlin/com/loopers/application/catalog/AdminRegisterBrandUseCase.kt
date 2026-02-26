package com.loopers.application.catalog

import com.loopers.application.UseCase
import com.loopers.domain.catalog.BrandInfo
import com.loopers.domain.catalog.BrandModel
import com.loopers.domain.catalog.BrandRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminRegisterBrandUseCase(
    private val brandRepository: BrandRepository,
) : UseCase<RegisterBrandCriteria, RegisterBrandResult> {

    @Transactional
    override fun execute(criteria: RegisterBrandCriteria): RegisterBrandResult {
        brandRepository.findByName(criteria.name)?.let {
            throw CoreException(ErrorType.CONFLICT, "이미 존재하는 브랜드입니다.")
        }
        val brand = BrandModel(
            name = criteria.name,
            description = criteria.description,
            logoUrl = criteria.logoUrl,
        )
        val saved = brandRepository.save(brand)
        val info = BrandInfo.from(saved)
        return RegisterBrandResult.from(info)
    }
}
