package com.loopers.interfaces.apiadmin.brand

import com.loopers.application.brand.AdminBrandFacade
import com.loopers.interfaces.common.ApiResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/brands")
class AdminBrandController(
    private val adminBrandFacade: AdminBrandFacade,
) : AdminBrandApiSpec {

    @GetMapping
    override fun getBrands(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<Page<AdminBrandDto.BrandResponse>> {
        val pageable = PageRequest.of(page, size)
        return adminBrandFacade.getBrands(pageable)
            .map { AdminBrandDto.BrandResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PostMapping
    override fun createBrand(
        @RequestBody request: AdminBrandDto.CreateRequest,
    ): ApiResponse<AdminBrandDto.CreateResponse> {
        return adminBrandFacade.createBrand(request.name, request.description)
            .let { AdminBrandDto.CreateResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{brandId}")
    override fun getBrand(
        @PathVariable brandId: Long,
    ): ApiResponse<AdminBrandDto.DetailResponse> {
        return adminBrandFacade.getBrand(brandId)
            .let { AdminBrandDto.DetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
