package com.loopers.interfaces.api.brand

import com.loopers.application.brand.BrandFacade
import com.loopers.interfaces.api.ApiResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
    private val brandFacade: BrandFacade,
) : BrandAdminV1ApiSpec {

    @GetMapping
    override fun getBrands(pageable: Pageable): ApiResponse<Page<BrandAdminV1Dto.BrandResponse>> =
        brandFacade.getBrands(pageable)
            .map { BrandAdminV1Dto.BrandResponse.from(it) }
            .let { ApiResponse.success(it) }

    @GetMapping("/{brandId}")
    override fun getBrandById(@PathVariable brandId: Long): ApiResponse<BrandAdminV1Dto.BrandResponse> =
        brandFacade.getBrandById(brandId)
            .let { BrandAdminV1Dto.BrandResponse.from(it) }
            .let { ApiResponse.success(it) }

    @PostMapping
    override fun createBrand(
        @RequestBody request: BrandAdminV1Dto.CreateBrandRequest,
    ): ApiResponse<BrandAdminV1Dto.BrandResponse> =
        brandFacade.createBrand(
            name = request.name,
            logoImageUrl = request.logoImageUrl,
            description = request.description,
            zipCode = request.zipCode,
            roadAddress = request.roadAddress,
            detailAddress = request.detailAddress,
            email = request.email,
            phoneNumber = request.phoneNumber,
            businessNumber = request.businessNumber,
        ).let { BrandAdminV1Dto.BrandResponse.from(it) }
         .let { ApiResponse.success(it) }

    @PutMapping("/{brandId}")
    override fun updateBrand(
        @PathVariable brandId: Long,
        @RequestBody request: BrandAdminV1Dto.UpdateBrandRequest,
    ): ApiResponse<BrandAdminV1Dto.BrandResponse> =
        brandFacade.updateBrand(
            id = brandId,
            name = request.name,
            logoImageUrl = request.logoImageUrl,
            description = request.description,
            zipCode = request.zipCode,
            roadAddress = request.roadAddress,
            detailAddress = request.detailAddress,
            email = request.email,
            phoneNumber = request.phoneNumber,
            businessNumber = request.businessNumber,
        ).let { BrandAdminV1Dto.BrandResponse.from(it) }
         .let { ApiResponse.success(it) }

    @DeleteMapping("/{brandId}")
    override fun deleteBrand(@PathVariable brandId: Long): ApiResponse<Unit> =
        brandFacade.deleteBrand(brandId).let { ApiResponse.success(Unit) }
}
