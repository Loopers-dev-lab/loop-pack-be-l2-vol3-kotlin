package com.loopers.interfaces.api.admin.coupon

import com.loopers.application.coupon.CouponFacade
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/coupons")
class AdminCouponV1Controller(
    private val couponFacade: CouponFacade,
) : AdminCouponV1ApiSpec {
    @GetMapping
    override fun getCouponTemplates(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResponse<AdminCouponV1Dto.CouponTemplateResponse>> {
        return couponFacade.getCouponTemplates(PageRequest.of(page, size))
            .map { AdminCouponV1Dto.CouponTemplateResponse.from(it) }
            .let { PageResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{couponId}")
    override fun getCouponTemplate(@PathVariable couponId: Long): ApiResponse<AdminCouponV1Dto.CouponTemplateResponse> {
        return couponFacade.getCouponTemplate(couponId)
            .let { AdminCouponV1Dto.CouponTemplateResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun createCouponTemplate(
        @RequestBody req: AdminCouponV1Dto.CreateCouponTemplateRequest,
    ): ApiResponse<AdminCouponV1Dto.CouponTemplateResponse> {
        return couponFacade.createCouponTemplate(req.name, req.type, req.value, req.minOrderAmount, req.expiredAt)
            .let { AdminCouponV1Dto.CouponTemplateResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/{couponId}")
    override fun updateCouponTemplate(
        @PathVariable couponId: Long,
        @RequestBody req: AdminCouponV1Dto.UpdateCouponTemplateRequest,
    ): ApiResponse<AdminCouponV1Dto.CouponTemplateResponse> {
        return couponFacade.updateCouponTemplate(couponId, req.name, req.value, req.minOrderAmount, req.expiredAt)
            .let { AdminCouponV1Dto.CouponTemplateResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{couponId}")
    override fun deleteCouponTemplate(@PathVariable couponId: Long): ApiResponse<Any> {
        couponFacade.deleteCouponTemplate(couponId)
        return ApiResponse.success()
    }

    @GetMapping("/{couponId}/issues")
    override fun getIssuedCoupons(
        @PathVariable couponId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResponse<AdminCouponV1Dto.IssuedCouponResponse>> {
        return couponFacade.getIssuedCoupons(couponId, PageRequest.of(page, size))
            .map { AdminCouponV1Dto.IssuedCouponResponse.from(it) }
            .let { PageResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
