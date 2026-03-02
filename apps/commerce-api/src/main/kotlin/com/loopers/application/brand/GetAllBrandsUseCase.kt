package com.loopers.application.brand

import com.loopers.domain.brand.BrandRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetAllBrandsUseCase(
    private val brandRepository: BrandRepository,
) {

    @Transactional(readOnly = true)
    fun execute(): List<BrandInfo> {
        return brandRepository.findAllActive().map { BrandInfo.from(it) }
    }
}
