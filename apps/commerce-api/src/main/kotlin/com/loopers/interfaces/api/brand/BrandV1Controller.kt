package com.loopers.interfaces.api.brand

import com.loopers.application.brand.BrandFacade
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.constant.ApiPaths
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(ApiPaths.Brands.BASE)
class BrandV1Controller(
    private val brandFacade: BrandFacade,
) {

    @GetMapping
    fun getAllBrands(): ApiResponse<List<BrandResponse>> {
        val brands = brandFacade.getAllActiveBrands()
        return ApiResponse.success(brands.map { BrandResponse.from(it) })
    }

    @GetMapping("/{brandId}")
    fun getBrand(@PathVariable brandId: Long): ApiResponse<BrandResponse> {
        val brand = brandFacade.getActiveBrand(brandId)
        return ApiResponse.success(BrandResponse.from(brand))
    }
}
