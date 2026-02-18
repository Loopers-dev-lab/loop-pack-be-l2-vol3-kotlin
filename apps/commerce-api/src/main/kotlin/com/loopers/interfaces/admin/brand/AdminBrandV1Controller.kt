package com.loopers.interfaces.admin.brand

import com.loopers.application.admin.brand.AdminBrandFacade
import com.loopers.domain.brand.dto.BrandInfo
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
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
class AdminBrandV1Controller(
    private val adminBrandFacade: AdminBrandFacade,
) : AdminBrandV1ApiSpec {

    @GetMapping("/{brandId}")
    override fun getBrandInfo(
        @PathVariable("brandId") brandId: Long,
    ): ApiResponse<BrandInfo> = ApiResponse.success(data = adminBrandFacade.getBrandInfo(brandId))

    @GetMapping
    override fun getAllBrands(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<Page<BrandInfo>> {
        if (size !in listOf(20, 50, 100)) {
            throw CoreException(ErrorType.BAD_REQUEST, "size는 20, 50, 100만 가능합니다")
        }

        val pageable = PageRequest.of(page, size)
        return ApiResponse.success(data = adminBrandFacade.getAllBrands(pageable))
    }

    @PostMapping
    override fun createBrand(
        @RequestBody request: AdminBrandV1Dto.CreateBrandRequest,
    ): ApiResponse<BrandInfo> {
        val brand = adminBrandFacade.createBrand(request.name, request.description)
        return ApiResponse.success(data = BrandInfo.from(brand))
    }

    @PutMapping("/{brandId}")
    override fun updateBrand(
        @PathVariable("brandId") brandId: Long,
        @RequestBody request: AdminBrandV1Dto.UpdateBrandRequest,
    ): ApiResponse<Unit> {
        adminBrandFacade.updateBrand(brandId, request.name, request.description)
        return ApiResponse.success(data = Unit)
    }

    @DeleteMapping("/{brandId}")
    override fun deleteBrand(
        @PathVariable("brandId") brandId: Long,
    ): ApiResponse<Unit> {
        adminBrandFacade.deleteBrand(brandId)
        return ApiResponse.success(data = Unit)
    }
}
