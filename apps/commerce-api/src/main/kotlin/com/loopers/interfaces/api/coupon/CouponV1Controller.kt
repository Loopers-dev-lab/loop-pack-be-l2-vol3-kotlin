package com.loopers.interfaces.api.coupon

import com.loopers.application.api.coupon.CouponFacade
import com.loopers.domain.coupon.dto.CouponInfo
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import com.loopers.support.validator.PageValidator
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/coupons")
class CouponV1Controller(
    private val couponFacade: CouponFacade,
) : CouponV1ApiSpec {

    @PostMapping("/{couponId}/issue")
    @ResponseStatus(HttpStatus.OK)
    override fun issueCoupon(
        @PathVariable couponId: Long,
        @RequestAttribute("userId") userId: Long,
    ): ApiResponse<CouponInfo> {
        val couponInfo = couponFacade.issueCoupon(userId, couponId)
        return ApiResponse.success(data = couponInfo)
    }

    @GetMapping
    override fun getMyCoupons(
        @RequestAttribute("userId") userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResponse<CouponInfo>> {
        PageValidator.validatePageRequest(page, size)

        val pageable = PageRequest.of(page, size)
        val couponInfoPage = couponFacade.getMyCoupons(userId, pageable)
        return ApiResponse.success(data = PageResponse.from(couponInfoPage))
    }
}
