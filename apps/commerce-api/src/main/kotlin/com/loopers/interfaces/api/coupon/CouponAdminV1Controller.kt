package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponFacade
import com.loopers.interfaces.api.ApiResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/coupons")
class CouponAdminV1Controller(
    private val couponFacade: CouponFacade,
) : CouponAdminV1ApiSpec {

    @GetMapping
    override fun getCoupons(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<Page<CouponAdminV1Dto.CouponAdminResponse>> {
        val pageable = PageRequest.of(page, size)
        return couponFacade.getCoupons(pageable)
            .map { CouponAdminV1Dto.CouponAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{couponId}")
    override fun getCoupon(
        @PathVariable couponId: Long,
    ): ApiResponse<CouponAdminV1Dto.CouponAdminResponse> {
        return couponFacade.getCoupon(couponId)
            .let { CouponAdminV1Dto.CouponAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PostMapping
    override fun createCoupon(
        @RequestBody request: CouponAdminV1Dto.CreateCouponRequest,
    ): ApiResponse<CouponAdminV1Dto.CouponAdminResponse> {
        return couponFacade.createCoupon(request.toCriteria())
            .let { CouponAdminV1Dto.CouponAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/{couponId}")
    override fun updateCoupon(
        @PathVariable couponId: Long,
        @RequestBody request: CouponAdminV1Dto.UpdateCouponRequest,
    ): ApiResponse<CouponAdminV1Dto.CouponAdminResponse> {
        return couponFacade.updateCoupon(couponId, request.toCriteria())
            .let { CouponAdminV1Dto.CouponAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{couponId}")
    override fun deleteCoupon(
        @PathVariable couponId: Long,
    ): ApiResponse<Unit> {
        couponFacade.deleteCoupon(couponId)
        return ApiResponse.success(Unit)
    }

    @GetMapping("/{couponId}/issues")
    override fun getCouponIssues(
        @PathVariable couponId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<Page<CouponAdminV1Dto.CouponIssueAdminResponse>> {
        val pageable = PageRequest.of(page, size)
        return couponFacade.getCouponIssues(couponId, pageable)
            .map { CouponAdminV1Dto.CouponIssueAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
