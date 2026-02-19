package com.loopers.interfaces.api.brand

import com.loopers.domain.catalog.CatalogService
import com.loopers.interfaces.api.brand.dto.BrandAdminV1Dto
import com.loopers.interfaces.api.brand.spec.BrandAdminV1ApiSpec
import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.toSpringPage
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import org.springframework.data.domain.Page
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
    private val catalogService: CatalogService,
) : BrandAdminV1ApiSpec {

    @GetMapping
    override fun getBrands(
        @RequestParam(defaultValue = "0") @PositiveOrZero page: Int,
        @RequestParam(defaultValue = "20") @Positive @Max(100) size: Int,
    ): ApiResponse<Page<BrandAdminV1Dto.BrandAdminResponse>> {
        return catalogService.getBrands(page, size)
            .map { BrandAdminV1Dto.BrandAdminResponse.from(it) }
            .toSpringPage()
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{brandId}")
    override fun getBrand(
        @PathVariable brandId: Long,
    ): ApiResponse<BrandAdminV1Dto.BrandAdminResponse> {
        return catalogService.getBrand(brandId)
            .let { BrandAdminV1Dto.BrandAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PostMapping
    override fun createBrand(
        @RequestBody @Valid request: BrandAdminV1Dto.CreateBrandRequest,
    ): ApiResponse<BrandAdminV1Dto.BrandAdminResponse> {
        return catalogService.createBrand(request.toCommand())
            .let { BrandAdminV1Dto.BrandAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/{brandId}")
    override fun updateBrand(
        @PathVariable brandId: Long,
        @RequestBody @Valid request: BrandAdminV1Dto.UpdateBrandRequest,
    ): ApiResponse<BrandAdminV1Dto.BrandAdminResponse> {
        return catalogService.updateBrand(brandId, request.toCommand())
            .let { BrandAdminV1Dto.BrandAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{brandId}")
    override fun deleteBrand(
        @PathVariable brandId: Long,
    ): ApiResponse<Any> {
        catalogService.deleteBrand(brandId)
        return ApiResponse.success()
    }
}
