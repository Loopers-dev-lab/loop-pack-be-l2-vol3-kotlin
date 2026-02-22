package com.loopers.interfaces.api.brand

import com.loopers.domain.brand.BrandService
import com.loopers.domain.brand.RegisterCommand
import com.loopers.interfaces.api.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/brands")
class BrandV1AdminController(
    private val brandService: BrandService,
) : BrandV1AdminApiSpec {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun register(
        @RequestHeader("X-Loopers-Ldap") ldap: String,
        @RequestBody request: BrandV1AdminDto.RegisterRequest,
    ) {
        brandService.register(
            RegisterCommand(
                name = request.name,
                description = request.description,
                logoUrl = request.logoUrl,
            ),
        )
    }

    override fun getBrands(ldap: String, page: Int, size: Int): ApiResponse<List<BrandV1AdminDto.BrandResponse>> {
        TODO("Not yet implemented")
    }

    override fun getBrand(ldap: String, brandId: Long): ApiResponse<BrandV1AdminDto.BrandResponse> {
        TODO("Not yet implemented")
    }

    override fun modifyBrand(ldap: String, brandId: Long, request: BrandV1AdminDto.RegisterRequest) {
        TODO("Not yet implemented")
    }

    override fun deleteBrand(ldap: String, brandId: Long) {
        TODO("Not yet implemented")
    }
}

