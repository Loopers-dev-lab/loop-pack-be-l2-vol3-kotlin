package com.loopers.application.brand

import com.loopers.domain.brand.BrandRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetBrandListUseCase(
    private val brandRepository: BrandRepository,
) {
    fun getAll(): List<BrandInfo> {
        return brandRepository.findAll().map { BrandInfo.from(it) }
    }

    fun getAllActive(): List<BrandInfo> {
        return brandRepository.findAllActive().map { BrandInfo.from(it) }
    }
}
