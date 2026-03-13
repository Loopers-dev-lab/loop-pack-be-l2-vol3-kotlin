package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponAdminFacade
import com.loopers.application.coupon.CouponAdminInfo
import com.loopers.application.coupon.CouponIssueAdminInfo
import com.loopers.interfaces.api.ApiResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/coupons")
class CouponAdminV1Controller(
    private val couponAdminFacade: CouponAdminFacade,
) {
    @GetMapping
    fun findAll(
        @PageableDefault(size = 20) pageable: Pageable,
    ): ApiResponse<Page<CouponAdminInfo>> {
        return ApiResponse.success(couponAdminFacade.findAll(pageable))
    }

    @GetMapping("/{couponId}")
    fun findById(
        @PathVariable couponId: Long,
    ): ApiResponse<CouponAdminInfo> {
        return ApiResponse.success(couponAdminFacade.findById(couponId))
    }

    @PostMapping
    fun create(
        @RequestBody request: CouponAdminV1Dto.CreateRequest,
    ): ApiResponse<CouponAdminInfo> {
        val result = couponAdminFacade.create(
            name = request.name,
            type = request.type,
            value = request.value,
            minOrderAmount = request.minOrderAmount,
            expiredAt = request.expiredAt,
        )
        return ApiResponse.success(result)
    }

    @PutMapping("/{couponId}")
    fun update(
        @PathVariable couponId: Long,
        @RequestBody request: CouponAdminV1Dto.UpdateRequest,
    ): ApiResponse<CouponAdminInfo> {
        val result = couponAdminFacade.update(
            id = couponId,
            name = request.name,
            type = request.type,
            value = request.value,
            minOrderAmount = request.minOrderAmount,
            expiredAt = request.expiredAt,
        )
        return ApiResponse.success(result)
    }

    @DeleteMapping("/{couponId}")
    fun delete(
        @PathVariable couponId: Long,
    ): ApiResponse<Any> {
        couponAdminFacade.delete(couponId)
        return ApiResponse.success()
    }

    @GetMapping("/{couponId}/issues")
    fun findIssues(
        @PathVariable couponId: Long,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ApiResponse<Page<CouponIssueAdminInfo>> {
        return ApiResponse.success(couponAdminFacade.findIssuesByCouponId(couponId, pageable))
    }
}
