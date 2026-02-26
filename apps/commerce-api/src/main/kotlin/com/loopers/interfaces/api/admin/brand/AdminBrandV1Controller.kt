package com.loopers.interfaces.api.admin.brand

import com.loopers.application.brand.AdminBrandFacade
import com.loopers.interfaces.config.auth.AdminAuthenticated
import com.loopers.domain.brand.BrandCommand
import com.loopers.interfaces.api.ApiResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@AdminAuthenticated
@RestController
@RequestMapping("/api-admin/v1/brands")
class AdminBrandV1Controller(
    private val adminBrandFacade: AdminBrandFacade,
) : AdminBrandV1ApiSpec {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun createBrand(
        @RequestBody @Valid request: AdminBrandV1Dto.CreateRequest,
    ): ApiResponse<AdminBrandV1Dto.BrandResponse> {
        val command = BrandCommand.Create(
            name = request.name,
            description = request.description,
            imageUrl = request.imageUrl,
        )
        return adminBrandFacade.createBrand(command)
            .let { AdminBrandV1Dto.BrandResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping
    override fun getBrands(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<Page<AdminBrandV1Dto.BrandResponse>> {
        return adminBrandFacade.getBrands(page, size)
            .map { AdminBrandV1Dto.BrandResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{brandId}")
    override fun getBrand(
        @PathVariable brandId: Long,
    ): ApiResponse<AdminBrandV1Dto.BrandResponse> {
        return adminBrandFacade.getBrand(brandId)
            .let { AdminBrandV1Dto.BrandResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/{brandId}")
    override fun updateBrand(
        @PathVariable brandId: Long,
        @RequestBody @Valid request: AdminBrandV1Dto.UpdateRequest,
    ): ApiResponse<AdminBrandV1Dto.BrandResponse> {
        val command = BrandCommand.Update(
            name = request.name,
            description = request.description,
            imageUrl = request.imageUrl,
        )
        return adminBrandFacade.updateBrand(brandId, command)
            .let { AdminBrandV1Dto.BrandResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{brandId}")
    override fun deleteBrand(
        @PathVariable brandId: Long,
    ): ApiResponse<Any> {
        adminBrandFacade.deleteBrand(brandId)
        return ApiResponse.success()
    }
}
