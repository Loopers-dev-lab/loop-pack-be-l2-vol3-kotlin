package com.loopers.interfaces.api.brand

import com.loopers.application.brand.BrandFacade
import com.loopers.interfaces.api.ApiResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/brands")
class BrandAdminV1Controller(
    private val brandFacade: BrandFacade,
) : BrandAdminV1ApiSpec {

    @GetMapping
    override fun getBrands(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<Page<BrandV1Dto.BrandResponse>> {
        val pageable = PageRequest.of(page, size)
        return brandFacade.getBrands(pageable)
            .map { BrandV1Dto.BrandResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{brandId}")
    override fun getBrand(
        @PathVariable brandId: Long,
    ): ApiResponse<BrandV1Dto.BrandResponse> {
        return brandFacade.getBrand(brandId)
            .let { BrandV1Dto.BrandResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PostMapping
    override fun createBrand(
        @RequestBody request: BrandV1Dto.CreateRequest,
    ): ApiResponse<BrandV1Dto.BrandResponse> {
        return brandFacade.createBrand(request.toCriteria())
            .let { BrandV1Dto.BrandResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/{brandId}")
    override fun updateBrand(
        @PathVariable brandId: Long,
        @RequestBody request: BrandV1Dto.UpdateRequest,
    ): ApiResponse<BrandV1Dto.BrandResponse> {
        return brandFacade.updateBrand(brandId, request.toCriteria())
            .let { BrandV1Dto.BrandResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{brandId}")
    override fun deleteBrand(
        @PathVariable brandId: Long,
    ): ApiResponse<Unit> {
        brandFacade.deleteBrand(brandId)
        return ApiResponse.success(Unit)
    }
}
