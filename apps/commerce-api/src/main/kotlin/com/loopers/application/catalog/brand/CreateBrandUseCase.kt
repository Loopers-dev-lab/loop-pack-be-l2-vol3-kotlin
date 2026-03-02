package com.loopers.application.catalog.brand

import com.loopers.domain.catalog.brand.model.Brand
import com.loopers.domain.catalog.brand.repository.BrandRepository
import com.loopers.domain.catalog.brand.vo.BrandName
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CreateBrandUseCase(private val brandRepository: BrandRepository) {
    @Transactional
    fun execute(name: String): BrandInfo {
        if (brandRepository.existsByName(name)) {
            throw CoreException(ErrorType.CONFLICT, "이미 존재하는 브랜드명입니다.")
        }
        val brand = brandRepository.save(Brand(name = BrandName(name)))
        return BrandInfo.from(brand)
    }
}
