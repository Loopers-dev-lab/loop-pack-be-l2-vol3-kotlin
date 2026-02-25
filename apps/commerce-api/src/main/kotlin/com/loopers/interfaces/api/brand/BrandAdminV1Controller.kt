package com.loopers.interfaces.api.brand

import com.loopers.application.brand.BrandFacade
import com.loopers.application.brand.BrandService
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/brands")
class BrandAdminV1Controller(
    private val brandService: BrandService,
    private val brandFacade: BrandFacade,
) : BrandAdminV1ApiSpec {

    @GetMapping
    override fun getAllBrands(
        @RequestHeader("X-Loopers-Ldap", required = false) ldap: String?,
        pageable: Pageable,
    ): ApiResponse<Page<BrandV1Dto.BrandAdminResponse>> {
        validateAdminAuth(ldap)
        return brandService.getAllBrands(pageable)
            .map { BrandV1Dto.BrandAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{brandId}")
    override fun getBrand(
        @RequestHeader("X-Loopers-Ldap", required = false) ldap: String?,
        @PathVariable brandId: Long,
    ): ApiResponse<BrandV1Dto.BrandAdminResponse> {
        validateAdminAuth(ldap)
        return brandService.getBrand(brandId)
            .let { BrandV1Dto.BrandAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PostMapping
    override fun createBrand(
        @RequestHeader("X-Loopers-Ldap", required = false) ldap: String?,
        @RequestBody request: BrandV1Dto.CreateRequest,
    ): ApiResponse<BrandV1Dto.BrandAdminResponse> {
        validateAdminAuth(ldap)
        return brandService.createBrand(request.toCommand())
            .let { BrandV1Dto.BrandAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/{brandId}")
    override fun updateBrand(
        @RequestHeader("X-Loopers-Ldap", required = false) ldap: String?,
        @PathVariable brandId: Long,
        @RequestBody request: BrandV1Dto.UpdateRequest,
    ): ApiResponse<BrandV1Dto.BrandAdminResponse> {
        validateAdminAuth(ldap)
        return brandService.updateBrand(brandId, request.toCommand())
            .let { BrandV1Dto.BrandAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{brandId}")
    override fun deleteBrand(
        @RequestHeader("X-Loopers-Ldap", required = false) ldap: String?,
        @PathVariable brandId: Long,
    ): ApiResponse<Any> {
        validateAdminAuth(ldap)
        brandFacade.deleteBrand(brandId)
        return ApiResponse.success()
    }

    private fun validateAdminAuth(ldap: String?) {
        if (ldap == null || ldap != ADMIN_LDAP) {
            throw CoreException(ErrorType.UNAUTHORIZED, "어드민 인증에 실패했습니다.")
        }
    }

    companion object {
        private const val ADMIN_LDAP = "loopers.admin"
    }
}
