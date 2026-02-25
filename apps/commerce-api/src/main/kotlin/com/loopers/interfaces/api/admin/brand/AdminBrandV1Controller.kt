package com.loopers.interfaces.api.admin.brand

import com.loopers.application.brand.BrandFacade
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AdminAuth
import com.loopers.support.constant.ApiPaths
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(ApiPaths.AdminBrands.BASE)
class AdminBrandV1Controller(
    private val brandFacade: BrandFacade,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @AdminAuth adminAuth: Unit,
        @Valid @RequestBody request: AdminBrandRegisterRequest,
    ): ApiResponse<AdminBrandResponse> {
        val brandInfo = brandFacade.register(request.toCommand())
        return ApiResponse.success(AdminBrandResponse.from(brandInfo))
    }

    @GetMapping
    fun getAllBrands(
        @AdminAuth adminAuth: Unit,
    ): ApiResponse<List<AdminBrandResponse>> {
        val brands = brandFacade.getAllActiveBrands()
        return ApiResponse.success(brands.map { AdminBrandResponse.from(it) })
    }

    @GetMapping("/{brandId}")
    fun getBrand(
        @AdminAuth adminAuth: Unit,
        @PathVariable brandId: Long,
    ): ApiResponse<AdminBrandResponse> {
        val brand = brandFacade.getActiveBrand(brandId)
        return ApiResponse.success(AdminBrandResponse.from(brand))
    }

    @PutMapping("/{brandId}")
    fun update(
        @AdminAuth adminAuth: Unit,
        @PathVariable brandId: Long,
        @Valid @RequestBody request: AdminBrandUpdateRequest,
    ): ApiResponse<AdminBrandResponse> {
        val brandInfo = brandFacade.update(request.toCommand(brandId))
        return ApiResponse.success(AdminBrandResponse.from(brandInfo))
    }

    @DeleteMapping("/{brandId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @AdminAuth adminAuth: Unit,
        @PathVariable brandId: Long,
    ): ApiResponse<Unit> {
        brandFacade.delete(brandId)
        return ApiResponse.success(Unit)
    }
}
