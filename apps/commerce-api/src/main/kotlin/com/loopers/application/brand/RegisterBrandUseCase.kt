package com.loopers.application.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandName
import com.loopers.domain.brand.BrandRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RegisterBrandUseCase(
    private val brandRepository: BrandRepository,
) {
    @Transactional
    fun register(command: RegisterBrandCommand): Long {
        val brandName = BrandName(command.name)

        if (brandRepository.existsByName(brandName)) {
            throw CoreException(ErrorType.CONFLICT, "이미 사용 중인 브랜드명입니다: ${brandName.value}")
        }

        val brand = Brand.create(
            name = brandName,
            description = command.description,
            logoUrl = command.logoUrl,
        )

        return brandRepository.save(brand)
    }
}
