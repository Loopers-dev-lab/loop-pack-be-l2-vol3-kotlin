package com.loopers.interfaces.api.admin.v1.brand

import com.loopers.application.brand.DeleteBrandUseCase
import com.loopers.application.brand.GetBrandListUseCase
import com.loopers.application.brand.GetBrandUseCase
import com.loopers.application.brand.RegisterBrandUseCase
import com.loopers.application.brand.UpdateBrandUseCase
import com.loopers.interfaces.api.ApiResponse
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
@RequestMapping("/api-admin/v1/brands")
class AdminBrandController(
    private val registerBrandUseCase: RegisterBrandUseCase,
    private val getBrandUseCase: GetBrandUseCase,
    private val getBrandListUseCase: GetBrandListUseCase,
    private val updateBrandUseCase: UpdateBrandUseCase,
    private val deleteBrandUseCase: DeleteBrandUseCase,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: CreateBrandRequest,
    ): ApiResponse<CreateBrandResponse> {
        val id = registerBrandUseCase.register(request.toCommand())
        return ApiResponse.success(CreateBrandResponse(id))
    }

    @GetMapping
    fun getAll(): ApiResponse<List<AdminBrandResponse>> {
        val brands = getBrandListUseCase.getAll()
        return ApiResponse.success(brands.map { AdminBrandResponse.from(it) })
    }

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: Long,
    ): ApiResponse<AdminBrandResponse> {
        val brandInfo = getBrandUseCase.getById(id)
        return ApiResponse.success(AdminBrandResponse.from(brandInfo))
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateBrandRequest,
    ): ApiResponse<AdminBrandResponse> {
        val brandInfo = updateBrandUseCase.update(id, request.toCommand())
        return ApiResponse.success(AdminBrandResponse.from(brandInfo))
    }

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: Long,
    ): ApiResponse<Nothing?> {
        deleteBrandUseCase.delete(id)
        return ApiResponse.success(null)
    }
}
