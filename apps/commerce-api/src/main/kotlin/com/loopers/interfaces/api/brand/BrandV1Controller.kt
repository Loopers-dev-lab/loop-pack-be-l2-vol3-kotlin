package com.loopers.interfaces.api.brand

import com.loopers.application.api.brand.BrandFacade
import com.loopers.domain.brand.dto.BrandInfo
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/brands")
class BrandV1Controller(
    private val brandFacade: BrandFacade,
) : BrandV1ApiSpec {

    @GetMapping("/{brandId}")
    override fun getBrandInfo(
        @PathVariable("brandId") brandId: Long,
    ): ApiResponse<BrandInfo> = ApiResponse.success(data = brandFacade.getBrandInfo(brandId))
}
