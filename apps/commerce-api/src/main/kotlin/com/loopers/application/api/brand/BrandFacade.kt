package com.loopers.application.api.brand

import com.loopers.domain.brand.BrandService
import com.loopers.domain.brand.dto.BrandInfo
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class BrandFacade(
    private val brandService: BrandService,
) {
    fun getBrandInfo(id: Long): BrandInfo = brandService.getBrandInfo(id)
}
