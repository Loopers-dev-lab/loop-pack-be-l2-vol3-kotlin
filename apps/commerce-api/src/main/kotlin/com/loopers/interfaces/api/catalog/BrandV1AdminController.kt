package com.loopers.interfaces.api.catalog

import com.loopers.application.catalog.AdminDeleteBrandUseCase
import com.loopers.application.catalog.AdminGetBrandUseCase
import com.loopers.application.catalog.AdminGetBrandsUseCase
import com.loopers.application.catalog.AdminRegisterBrandUseCase
import com.loopers.application.catalog.AdminUpdateBrandUseCase
import com.loopers.application.catalog.ListBrandsCriteria
import com.loopers.application.catalog.RegisterBrandCriteria
import com.loopers.application.catalog.UpdateBrandCriteria
import com.loopers.interfaces.api.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/brands")
class BrandV1AdminController(
    private val adminRegisterBrandUseCase: AdminRegisterBrandUseCase,
    private val adminGetBrandUseCase: AdminGetBrandUseCase,
    private val adminUpdateBrandUseCase: AdminUpdateBrandUseCase,
    private val adminGetBrandsUseCase: AdminGetBrandsUseCase,
    private val adminDeleteBrandUseCase: AdminDeleteBrandUseCase,
) : BrandV1AdminApiSpec {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun register(
        @RequestHeader("X-Loopers-Ldap") ldap: String,
        @RequestBody request: BrandV1AdminDto.RegisterRequest,
    ) {
        adminRegisterBrandUseCase.execute(
            RegisterBrandCriteria(
                name = request.name,
                description = request.description,
                logoUrl = request.logoUrl,
            ),
        )
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    override fun getBrands(
        @RequestHeader(value = "X-Loopers-Ldap") ldap: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<BrandV1AdminDto.BrandSliceResponse> {
        return adminGetBrandsUseCase.execute(ListBrandsCriteria(page = page, size = size))
            .let { BrandV1AdminDto.BrandSliceResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{brandId}")
    @ResponseStatus(HttpStatus.OK)
    override fun getBrand(
        @RequestHeader(value = "X-Loopers-Ldap") ldap: String,
        @PathVariable brandId: Long,
    ): ApiResponse<BrandV1AdminDto.BrandDetailResponse> {
        return adminGetBrandUseCase.execute(brandId)
            .let { BrandV1AdminDto.BrandDetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/{brandId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun modifyBrand(
        @RequestHeader(value = "X-Loopers-Ldap") ldap: String,
        @PathVariable brandId: Long,
        @RequestBody request: BrandV1AdminDto.UpdateRequest,
    ) {
        adminUpdateBrandUseCase.execute(
            UpdateBrandCriteria(
                brandId = brandId,
                newName = request.newName,
                newDescription = request.newDescription,
                newLogoUrl = request.newLogoUrl,
            ),
        )
    }

    @DeleteMapping("/{brandId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun deleteBrand(
        @RequestHeader(value = "X-Loopers-Ldap") ldap: String,
        @PathVariable brandId: Long,
    ) {
        adminDeleteBrandUseCase.execute(brandId)
    }
}
