package com.loopers.interfaces.api.v1.brand

import com.loopers.application.brand.GetBrandUseCase
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/brands")
class BrandController(
    private val getBrandUseCase: GetBrandUseCase,
) {
    @GetMapping("/{id}")
    fun getBrand(
        @PathVariable id: Long,
    ): ApiResponse<GetBrandResponse> {
        val brandInfo = getBrandUseCase.getActiveById(id)
        return ApiResponse.success(GetBrandResponse.from(brandInfo))
    }
}
