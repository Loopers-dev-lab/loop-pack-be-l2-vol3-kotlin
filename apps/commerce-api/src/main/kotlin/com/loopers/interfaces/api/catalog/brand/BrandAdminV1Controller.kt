package com.loopers.interfaces.api.catalog.brand

import com.loopers.application.catalog.brand.BrandFacade
import com.loopers.domain.catalog.brand.BrandRepository
import com.loopers.domain.catalog.brand.BrandService
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.security.AdminHeader
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/brands")
class BrandAdminV1Controller(
    private val brandFacade: BrandFacade,
    private val brandService: BrandService,
    private val brandRepository: BrandRepository,
) : BrandAdminV1ApiSpec {

    @GetMapping
    override fun getBrands(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<List<BrandAdminV1Dto.BrandResponse>> =
        brandRepository.findAll(page, size)
            .map { BrandAdminV1Dto.BrandResponse.from(it) }
            .let { ApiResponse.success(it) }

    @GetMapping("/{brandId}")
    override fun getBrand(@PathVariable brandId: Long): ApiResponse<BrandAdminV1Dto.BrandResponse> =
        brandService.getById(brandId)
            .let { BrandAdminV1Dto.BrandResponse.from(it) }
            .let { ApiResponse.success(it) }

    @PostMapping
    override fun createBrand(
        @RequestHeader(AdminHeader.HEADER_LDAP) ldap: String,
        @RequestBody request: BrandAdminV1Dto.CreateBrandRequest,
    ): ApiResponse<BrandAdminV1Dto.BrandResponse> {
        if (ldap != AdminHeader.LDAP_ADMIN_VALUE) throw CoreException(ErrorType.UNAUTHORIZED, "어드민 인증에 실패했습니다.")
        return brandService.createBrand(name = request.name, description = request.description)
            .let { BrandAdminV1Dto.BrandResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/{brandId}")
    override fun updateBrand(
        @RequestHeader(AdminHeader.HEADER_LDAP) ldap: String,
        @PathVariable brandId: Long,
        @RequestBody request: BrandAdminV1Dto.UpdateBrandRequest,
    ): ApiResponse<BrandAdminV1Dto.BrandResponse> {
        if (ldap != AdminHeader.LDAP_ADMIN_VALUE) throw CoreException(ErrorType.UNAUTHORIZED, "어드민 인증에 실패했습니다.")
        return brandService.update(brandId, name = request.name, description = request.description)
            .let { BrandAdminV1Dto.BrandResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{brandId}")
    override fun deleteBrand(
        @RequestHeader(AdminHeader.HEADER_LDAP) ldap: String,
        @PathVariable brandId: Long,
    ): ApiResponse<Any> {
        if (ldap != AdminHeader.LDAP_ADMIN_VALUE) throw CoreException(ErrorType.UNAUTHORIZED, "어드민 인증에 실패했습니다.")
        brandFacade.deleteBrand(brandId)
        return ApiResponse.success()
    }
}
