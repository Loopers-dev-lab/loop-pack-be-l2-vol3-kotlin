package com.loopers.interfaces.api.admin.brand

import com.loopers.application.brand.BrandFacade
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
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
    private val brandFacade: BrandFacade,
) : AdminBrandV1ApiSpec {
    @GetMapping
    override fun getBrands(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResponse<AdminBrandV1Dto.BrandResponse>> {
        return brandFacade.getBrands(PageRequest.of(page, size))
            .map { AdminBrandV1Dto.BrandResponse.from(it) }
            .let { PageResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{brandId}")
    override fun getBrand(@PathVariable brandId: Long): ApiResponse<AdminBrandV1Dto.BrandResponse> {
        return brandFacade.getBrand(brandId)
            .let { AdminBrandV1Dto.BrandResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PostMapping
    override fun createBrand(@RequestBody req: AdminBrandV1Dto.CreateBrandRequest): ApiResponse<AdminBrandV1Dto.BrandResponse> {
        return brandFacade.createBrand(req.name, req.description)
            .let { AdminBrandV1Dto.BrandResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/{brandId}")
    override fun updateBrand(
        @PathVariable brandId: Long,
        @RequestBody req: AdminBrandV1Dto.UpdateBrandRequest,
    ): ApiResponse<AdminBrandV1Dto.BrandResponse> {
        return brandFacade.updateBrand(brandId, req.name, req.description)
            .let { AdminBrandV1Dto.BrandResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{brandId}")
    override fun deleteBrand(@PathVariable brandId: Long): ApiResponse<Any> {
        brandFacade.deleteBrand(brandId)
        return ApiResponse.success()
    }
}
