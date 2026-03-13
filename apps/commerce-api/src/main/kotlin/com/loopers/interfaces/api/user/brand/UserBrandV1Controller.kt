package com.loopers.interfaces.api.user.brand

import com.loopers.application.user.brand.UserBrandDetailUseCase
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/brands")
@RestController
class UserBrandV1Controller(
    private val detailUseCase: UserBrandDetailUseCase,
) : UserBrandV1ApiSpec {

    @GetMapping("/{brandId}")
    override fun getDetail(
        @PathVariable brandId: Long,
    ): ApiResponse<UserBrandV1Response.Detail> {
        return detailUseCase.getDetail(brandId)
            .let { UserBrandV1Response.Detail.from(it) }
            .let { ApiResponse.success(it) }
    }
}
