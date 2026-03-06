package com.loopers.interfaces.api.brand

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.brand.BrandService
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/brands")
class BrandV1Controller(
    private val brandService: BrandService,
) {
    @GetMapping("/{brandId}")
    fun findById(
        @PathVariable brandId: Long,
    ): ApiResponse<BrandV1Dto.BrandResponse> {
        val brand = brandService.findById(brandId)
        return ApiResponse.success(BrandV1Dto.BrandResponse.from(brand))
    }
}

class BrandV1Dto {
    data class BrandResponse(
        val id: Long,
        val name: String,
        val description: String?,
        val logoUrl: String?,
    ) {
        companion object {
            fun from(brand: BrandModel): BrandResponse {
                return BrandResponse(
                    id = brand.id,
                    name = brand.name,
                    description = brand.description,
                    logoUrl = brand.logoUrl,
                )
            }
        }
    }
}
