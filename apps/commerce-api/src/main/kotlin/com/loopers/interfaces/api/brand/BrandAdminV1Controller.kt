package com.loopers.interfaces.api.brand

import com.loopers.application.catalog.brand.CreateBrandUseCase
import com.loopers.application.catalog.brand.DeleteBrandUseCase
import com.loopers.application.catalog.brand.GetBrandUseCase
import com.loopers.application.catalog.brand.GetBrandsUseCase
import com.loopers.application.catalog.brand.RestoreBrandUseCase
import com.loopers.application.catalog.brand.UpdateBrandUseCase
import com.loopers.interfaces.api.brand.dto.BrandAdminV1Dto
import com.loopers.interfaces.api.brand.spec.BrandAdminV1ApiSpec
import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.toSpringPage
import org.springframework.data.domain.Page
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api-admin/v1/brands")
class BrandAdminV1Controller(
    private val createBrandUseCase: CreateBrandUseCase,
    private val updateBrandUseCase: UpdateBrandUseCase,
    private val deleteBrandUseCase: DeleteBrandUseCase,
    private val restoreBrandUseCase: RestoreBrandUseCase,
    private val getBrandUseCase: GetBrandUseCase,
    private val getBrandsUseCase: GetBrandsUseCase,
) : BrandAdminV1ApiSpec {

    @GetMapping
    override fun getBrands(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<Page<BrandAdminV1Dto.BrandAdminResponse>> {
        return getBrandsUseCase.execute(page, size)
            .map { BrandAdminV1Dto.BrandAdminResponse.from(it) }
            .toSpringPage()
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{brandId}")
    override fun getBrand(
        @PathVariable brandId: Long,
    ): ApiResponse<BrandAdminV1Dto.BrandAdminResponse> {
        return getBrandUseCase.executeAdmin(brandId)
            .let { BrandAdminV1Dto.BrandAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PostMapping
    override fun createBrand(
        @RequestParam name: String,
    ): ApiResponse<BrandAdminV1Dto.BrandAdminResponse> {
        return createBrandUseCase.execute(name)
            .let { BrandAdminV1Dto.BrandAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/{brandId}")
    override fun updateBrand(
        @PathVariable brandId: Long,
        @RequestParam name: String,
    ): ApiResponse<BrandAdminV1Dto.BrandAdminResponse> {
        return updateBrandUseCase.execute(brandId, name)
            .let { BrandAdminV1Dto.BrandAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{brandId}")
    override fun deleteBrand(
        @PathVariable brandId: Long,
    ): ApiResponse<Any> {
        deleteBrandUseCase.execute(brandId)
        return ApiResponse.success()
    }

    @PostMapping("/{brandId}/restore")
    override fun restoreBrand(
        @PathVariable brandId: Long,
    ): ApiResponse<BrandAdminV1Dto.BrandAdminResponse> {
        return restoreBrandUseCase.execute(brandId)
            .let { BrandAdminV1Dto.BrandAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
