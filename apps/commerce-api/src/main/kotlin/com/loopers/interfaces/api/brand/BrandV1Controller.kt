package com.loopers.interfaces.api.brand

import com.loopers.application.catalog.brand.GetBrandUseCase
import com.loopers.interfaces.api.brand.dto.BrandV1Dto
import com.loopers.interfaces.api.brand.spec.BrandV1ApiSpec
import com.loopers.interfaces.support.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/brands")
class BrandV1Controller(
    private val getBrandUseCase: GetBrandUseCase,
) : BrandV1ApiSpec {

    @GetMapping("/{brandId}")
    override fun getBrand(
        @PathVariable brandId: Long,
    ): ApiResponse<BrandV1Dto.BrandResponse> {
        return getBrandUseCase.execute(brandId)
            .let { BrandV1Dto.BrandResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
