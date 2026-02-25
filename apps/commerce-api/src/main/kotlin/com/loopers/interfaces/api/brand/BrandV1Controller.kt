package com.loopers.interfaces.api.brand

import com.loopers.application.auth.AuthFacade
import com.loopers.application.brand.BrandFacade
import com.loopers.interfaces.api.ApiResponse
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
@RequestMapping("/api/v1/brands")
class BrandV1Controller(
    private val authFacade: AuthFacade,
    private val brandFacade: BrandFacade,
) : BrandV1ApiSpec {

    @PostMapping
    override fun register(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @RequestBody request: BrandV1Dto.RegisterRequest,
    ): ApiResponse<BrandV1Dto.DetailResponse> {
        authFacade.authenticate(loginId, password)

        return brandFacade.register(request.toCommand())
            .let { BrandV1Dto.DetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{id}")
    override fun getById(
        @PathVariable id: Long,
    ): ApiResponse<BrandV1Dto.DetailResponse> {
        return brandFacade.getById(id)
            .let { BrandV1Dto.DetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping
    override fun getAllActive(): ApiResponse<List<BrandV1Dto.MainResponse>> {
        return brandFacade.getAllActive()
            .map { BrandV1Dto.MainResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/{id}")
    override fun changeName(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @PathVariable id: Long,
        @RequestBody request: BrandV1Dto.ChangeNameRequest,
    ): ApiResponse<BrandV1Dto.DetailResponse> {
        authFacade.authenticate(loginId, password)

        return brandFacade.changeName(id, request.toCommand())
            .let { BrandV1Dto.DetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{id}")
    override fun remove(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @PathVariable id: Long,
    ): ApiResponse<Any> {
        authFacade.authenticate(loginId, password)

        brandFacade.remove(id)
        return ApiResponse.success()
    }
}
