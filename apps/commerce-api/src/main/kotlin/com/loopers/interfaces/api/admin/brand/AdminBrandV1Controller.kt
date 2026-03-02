package com.loopers.interfaces.api.admin.brand

import com.loopers.application.brand.DeleteBrandUseCase
import com.loopers.application.brand.GetAllBrandsUseCase
import com.loopers.application.brand.GetBrandUseCase
import com.loopers.application.brand.RegisterBrandUseCase
import com.loopers.application.brand.UpdateBrandUseCase
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
    private val registerBrandUseCase: RegisterBrandUseCase,
    private val updateBrandUseCase: UpdateBrandUseCase,
    private val deleteBrandUseCase: DeleteBrandUseCase,
    private val getBrandUseCase: GetBrandUseCase,
    private val getAllBrandsUseCase: GetAllBrandsUseCase,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @AdminAuth adminAuth: Unit,
        @Valid @RequestBody request: AdminBrandRegisterRequest,
    ): ApiResponse<AdminBrandResponse> {
        val brandInfo = registerBrandUseCase.execute(request.toCommand())
        return ApiResponse.success(AdminBrandResponse.from(brandInfo))
    }

    @GetMapping
    fun getAllBrands(
        @AdminAuth adminAuth: Unit,
    ): ApiResponse<List<AdminBrandResponse>> {
        val brands = getAllBrandsUseCase.execute()
        return ApiResponse.success(brands.map { AdminBrandResponse.from(it) })
    }

    @GetMapping("/{brandId}")
    fun getBrand(
        @AdminAuth adminAuth: Unit,
        @PathVariable brandId: Long,
    ): ApiResponse<AdminBrandResponse> {
        val brand = getBrandUseCase.execute(brandId)
        return ApiResponse.success(AdminBrandResponse.from(brand))
    }

    @PutMapping("/{brandId}")
    fun update(
        @AdminAuth adminAuth: Unit,
        @PathVariable brandId: Long,
        @Valid @RequestBody request: AdminBrandUpdateRequest,
    ): ApiResponse<AdminBrandResponse> {
        val brandInfo = updateBrandUseCase.execute(request.toCommand(brandId))
        return ApiResponse.success(AdminBrandResponse.from(brandInfo))
    }

    @DeleteMapping("/{brandId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @AdminAuth adminAuth: Unit,
        @PathVariable brandId: Long,
    ): ApiResponse<Unit> {
        deleteBrandUseCase.execute(brandId)
        return ApiResponse.success(Unit)
    }
}
