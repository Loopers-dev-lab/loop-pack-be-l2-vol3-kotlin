package com.loopers.interfaces.api.brand

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.brand.BrandService
import com.loopers.domain.brand.BrandStatus
import com.loopers.interfaces.api.ApiResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/brands")
class BrandAdminV1Controller(
    private val brandService: BrandService,
) {
    @GetMapping
    fun findAll(
        @PageableDefault(size = 20) pageable: Pageable,
    ): ApiResponse<Page<BrandAdminV1Dto.BrandResponse>> {
        return ApiResponse.success(
            brandService.findAll(pageable).map { BrandAdminV1Dto.BrandResponse.from(it) },
        )
    }

    @GetMapping("/{brandId}")
    fun findById(@PathVariable brandId: Long): ApiResponse<BrandAdminV1Dto.BrandResponse> {
        return ApiResponse.success(BrandAdminV1Dto.BrandResponse.from(brandService.findById(brandId)))
    }

    @PostMapping
    fun create(@RequestBody request: BrandAdminV1Dto.CreateRequest): ApiResponse<BrandAdminV1Dto.BrandResponse> {
        val brand = brandService.create(request.name, request.description, request.logoUrl)
        return ApiResponse.success(BrandAdminV1Dto.BrandResponse.from(brand))
    }

    @PutMapping("/{brandId}")
    fun update(
        @PathVariable brandId: Long,
        @RequestBody request: BrandAdminV1Dto.UpdateRequest,
    ): ApiResponse<BrandAdminV1Dto.BrandResponse> {
        val brand = brandService.update(brandId, request.name, request.description, request.logoUrl, request.status)
        return ApiResponse.success(BrandAdminV1Dto.BrandResponse.from(brand))
    }

    @DeleteMapping("/{brandId}")
    fun delete(@PathVariable brandId: Long): ApiResponse<Any> {
        brandService.delete(brandId)
        return ApiResponse.success()
    }
}

class BrandAdminV1Dto {
    data class CreateRequest(
        val name: String,
        val description: String? = null,
        val logoUrl: String? = null,
    )

    data class UpdateRequest(
        val name: String,
        val description: String? = null,
        val logoUrl: String? = null,
        val status: BrandStatus = BrandStatus.ACTIVE,
    )

    data class BrandResponse(
        val id: Long,
        val name: String,
        val description: String?,
        val logoUrl: String?,
        val status: BrandStatus,
    ) {
        companion object {
            fun from(brand: BrandModel): BrandResponse {
                return BrandResponse(
                    id = brand.id,
                    name = brand.name,
                    description = brand.description,
                    logoUrl = brand.logoUrl,
                    status = brand.status,
                )
            }
        }
    }
}
