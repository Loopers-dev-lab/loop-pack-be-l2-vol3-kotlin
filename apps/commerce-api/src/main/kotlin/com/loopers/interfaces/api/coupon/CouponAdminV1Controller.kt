package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CreateCouponAdminUseCase
import com.loopers.application.coupon.DeleteCouponAdminUseCase
import com.loopers.application.coupon.GetCouponAdminUseCase
import com.loopers.application.coupon.GetCouponIssuesAdminUseCase
import com.loopers.application.coupon.GetCouponsAdminUseCase
import com.loopers.application.coupon.UpdateCouponAdminUseCase
import com.loopers.interfaces.api.coupon.dto.CouponAdminV1Dto
import com.loopers.interfaces.api.coupon.spec.CouponAdminV1ApiSpec
import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.toSpringPage
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api-admin/v1/coupons")
class CouponAdminV1Controller(
    private val createCouponAdminUseCase: CreateCouponAdminUseCase,
    private val updateCouponAdminUseCase: UpdateCouponAdminUseCase,
    private val deleteCouponAdminUseCase: DeleteCouponAdminUseCase,
    private val getCouponAdminUseCase: GetCouponAdminUseCase,
    private val getCouponsAdminUseCase: GetCouponsAdminUseCase,
    private val getCouponIssuesAdminUseCase: GetCouponIssuesAdminUseCase,
) : CouponAdminV1ApiSpec {

    @GetMapping
    override fun getCoupons(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<Page<CouponAdminV1Dto.CouponAdminResponse>> {
        return getCouponsAdminUseCase.execute(page, size)
            .map { CouponAdminV1Dto.CouponAdminResponse.from(it) }
            .toSpringPage()
            .let { ApiResponse.success(it) }
    }

    @PostMapping
    override fun createCoupon(
        @Valid @RequestBody request: CouponAdminV1Dto.CreateCouponRequest,
    ): ApiResponse<CouponAdminV1Dto.CouponAdminResponse> {
        return createCouponAdminUseCase.execute(request.toCommand())
            .let { CouponAdminV1Dto.CouponAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{couponId}")
    override fun getCoupon(
        @PathVariable couponId: Long,
    ): ApiResponse<CouponAdminV1Dto.CouponAdminResponse> {
        return getCouponAdminUseCase.execute(couponId)
            .let { CouponAdminV1Dto.CouponAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/{couponId}")
    override fun updateCoupon(
        @PathVariable couponId: Long,
        @Valid @RequestBody request: CouponAdminV1Dto.UpdateCouponRequest,
    ): ApiResponse<CouponAdminV1Dto.CouponAdminResponse> {
        return updateCouponAdminUseCase.execute(couponId, request.toCommand())
            .let { CouponAdminV1Dto.CouponAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{couponId}")
    override fun deleteCoupon(
        @PathVariable couponId: Long,
    ): ApiResponse<Any> {
        deleteCouponAdminUseCase.execute(couponId)
        return ApiResponse.success()
    }

    @GetMapping("/{couponId}/issues")
    override fun getCouponIssues(
        @PathVariable couponId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<Page<CouponAdminV1Dto.IssuedCouponAdminResponse>> {
        return getCouponIssuesAdminUseCase.execute(couponId, page, size)
            .map { CouponAdminV1Dto.IssuedCouponAdminResponse.from(it) }
            .toSpringPage()
            .let { ApiResponse.success(it) }
    }
}
