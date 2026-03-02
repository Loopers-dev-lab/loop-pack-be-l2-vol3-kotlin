package com.loopers.interfaces.apiadmin.coupon

import com.loopers.application.coupon.AdminCouponFacade
import com.loopers.interfaces.common.ApiResponse
import com.loopers.support.common.PageQuery
import com.loopers.support.common.SortOrder
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
class AdminCouponController(
    private val adminCouponFacade: AdminCouponFacade,
) : AdminCouponApiSpec {

    @PostMapping
    override fun createCoupon(
        @RequestBody request: AdminCouponDto.CreateRequest,
    ): ApiResponse<AdminCouponDto.DetailResponse> {
        return adminCouponFacade.createCoupon(
            name = request.name,
            discountType = request.discountType,
            discountValue = request.discountValue,
            totalQuantity = request.totalQuantity,
            expiresAt = request.expiresAt,
        )
            .let { AdminCouponDto.DetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/{couponId}")
    override fun updateCoupon(
        @PathVariable couponId: Long,
        @RequestBody request: AdminCouponDto.UpdateRequest,
    ): ApiResponse<AdminCouponDto.DetailResponse> {
        return adminCouponFacade.updateCoupon(
            couponId = couponId,
            name = request.name,
            discountType = request.discountType,
            discountValue = request.discountValue,
            expiresAt = request.expiresAt,
        )
            .let { AdminCouponDto.DetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping
    override fun getCoupons(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<AdminCouponDto.PageResponse> {
        val pageQuery = PageQuery(page, size, SortOrder.UNSORTED)
        return adminCouponFacade.getCoupons(pageQuery)
            .let { AdminCouponDto.PageResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{couponId}")
    override fun getCoupon(
        @PathVariable couponId: Long,
    ): ApiResponse<AdminCouponDto.DetailResponse> {
        return adminCouponFacade.getCoupon(couponId)
            .let { AdminCouponDto.DetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
