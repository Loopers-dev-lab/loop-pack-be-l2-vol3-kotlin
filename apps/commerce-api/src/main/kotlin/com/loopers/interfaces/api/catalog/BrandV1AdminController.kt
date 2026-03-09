package com.loopers.interfaces.api.catalog

import com.loopers.application.catalog.AdminDeleteBrandUseCase
import com.loopers.domain.catalog.BrandService
import com.loopers.domain.catalog.RegisterBrandCommand
import com.loopers.domain.catalog.UpdateBrandCommand
import com.loopers.interfaces.api.ApiResponse
import org.springframework.data.domain.PageRequest
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
    private val brandService: BrandService,
    private val adminDeleteBrandUseCase: AdminDeleteBrandUseCase,
) : BrandV1AdminApiSpec {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun register(
        @RequestHeader("X-Loopers-Ldap") ldap: String,
        @RequestBody request: BrandV1AdminDto.RegisterRequest,
    ) {
        brandService.register(
            RegisterBrandCommand(
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
        val pageable = PageRequest.of(page, size)
        return brandService.getBrands(pageable)
            .let { BrandV1AdminDto.BrandSliceResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{brandId}")
    @ResponseStatus(HttpStatus.OK)
    override fun getBrand(
        @RequestHeader(value = "X-Loopers-Ldap") ldap: String,
        @PathVariable brandId: Long,
    ): ApiResponse<BrandV1AdminDto.BrandDetailResponse> {
        return brandService.getBrand(brandId)
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
        brandService.update(
            brandId,
            UpdateBrandCommand(
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
