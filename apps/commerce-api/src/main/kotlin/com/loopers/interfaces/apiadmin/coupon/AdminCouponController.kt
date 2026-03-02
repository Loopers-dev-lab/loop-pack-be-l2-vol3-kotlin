package com.loopers.interfaces.apiadmin.coupon

import com.loopers.application.coupon.AdminCouponFacade
import com.loopers.support.common.PageQuery
import com.loopers.support.common.SortOrder
import com.loopers.interfaces.common.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/coupons")
class AdminCouponController(
    private val adminCouponFacade: AdminCouponFacade,
) : AdminCouponApiSpec {

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
}
