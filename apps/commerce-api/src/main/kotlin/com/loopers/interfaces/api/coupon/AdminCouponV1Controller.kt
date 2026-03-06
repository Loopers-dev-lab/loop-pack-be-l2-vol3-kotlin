package com.loopers.interfaces.api.coupon

import com.loopers.application.auth.AuthUseCase
import com.loopers.application.coupon.CouponAdminUseCase
import com.loopers.interfaces.api.ApiResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
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
@RequestMapping("/api-admin/v1/coupons")
class AdminCouponV1Controller(
    private val authUseCase: AuthUseCase,
    private val couponAdminUseCase: CouponAdminUseCase,
) : AdminCouponV1ApiSpec {

    @PostMapping
    override fun register(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @Valid @RequestBody request: AdminCouponV1Dto.RegisterRequest,
    ): ApiResponse<AdminCouponV1Dto.DetailResponse> {
        authUseCase.authenticate(loginId, password)

        return couponAdminUseCase.register(request.toCommand())
            .let { AdminCouponV1Dto.DetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping
    override fun getAll(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<Page<AdminCouponV1Dto.MainResponse>> {
        authUseCase.authenticate(loginId, password)

        return couponAdminUseCase.getAll(PageRequest.of(page, size))
            .map { AdminCouponV1Dto.MainResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{couponId}")
    override fun getById(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @PathVariable couponId: Long,
    ): ApiResponse<AdminCouponV1Dto.DetailResponse> {
        authUseCase.authenticate(loginId, password)

        return couponAdminUseCase.getById(couponId)
            .let { AdminCouponV1Dto.DetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/{couponId}")
    override fun update(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @PathVariable couponId: Long,
        @Valid @RequestBody request: AdminCouponV1Dto.UpdateRequest,
    ): ApiResponse<AdminCouponV1Dto.DetailResponse> {
        authUseCase.authenticate(loginId, password)

        return couponAdminUseCase.update(couponId, request.toCommand())
            .let { AdminCouponV1Dto.DetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{couponId}")
    override fun delete(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @PathVariable couponId: Long,
    ): ApiResponse<Any> {
        authUseCase.authenticate(loginId, password)

        couponAdminUseCase.delete(couponId)
        return ApiResponse.success()
    }

    @GetMapping("/{couponId}/issues")
    override fun getIssuedCoupons(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @PathVariable couponId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<Page<AdminCouponV1Dto.IssuedMainResponse>> {
        authUseCase.authenticate(loginId, password)

        return couponAdminUseCase.getIssuedCoupons(couponId, PageRequest.of(page, size))
            .map { AdminCouponV1Dto.IssuedMainResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
