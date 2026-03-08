package com.loopers.interfaces.admin.coupon

import com.loopers.application.admin.coupon.AdminCouponFacade
import com.loopers.domain.coupon.dto.CouponTemplateDetailInfo
import com.loopers.domain.coupon.dto.IssuedCouponInfo
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import com.loopers.support.validator.PageValidator
import jakarta.validation.Valid
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
    private val adminCouponFacade: AdminCouponFacade,
) : AdminCouponV1ApiSpec {

    @GetMapping
    override fun getCouponTemplates(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResponse<CouponTemplateDetailInfo>> {
        PageValidator.validatePageRequest(page, size)

        val pageable = PageRequest.of(page, size)
        val templatePage = adminCouponFacade.getCouponTemplates(pageable)
        return ApiResponse.success(data = PageResponse.from(templatePage))
    }

    @GetMapping("/{templateId}")
    override fun getCouponTemplate(
        @PathVariable templateId: Long,
    ): ApiResponse<CouponTemplateDetailInfo> {
        val templateInfo = adminCouponFacade.getCouponTemplate(templateId)
        return ApiResponse.success(data = templateInfo)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun createCouponTemplate(
        @RequestBody @Valid request: AdminCouponV1Dto.CreateCouponTemplateRequest,
    ): ApiResponse<CouponTemplateDetailInfo> {
        val templateInfo = adminCouponFacade.createCouponTemplate(
            name = request.name,
            type = request.type,
            value = request.value,
            minOrderAmount = request.minOrderAmount,
            expiredAt = request.expiredAt,
        )
        return ApiResponse.success(data = templateInfo)
    }

    @PutMapping("/{templateId}")
    override fun updateCouponTemplate(
        @PathVariable templateId: Long,
        @RequestBody @Valid request: AdminCouponV1Dto.UpdateCouponTemplateRequest,
    ): ApiResponse<CouponTemplateDetailInfo> {
        val templateInfo = adminCouponFacade.updateCouponTemplate(
            templateId = templateId,
            name = request.name,
            value = request.value,
            minOrderAmount = request.minOrderAmount,
            expiredAt = request.expiredAt,
        )
        return ApiResponse.success(data = templateInfo)
    }

    @DeleteMapping("/{templateId}")
    override fun deleteCouponTemplate(
        @PathVariable templateId: Long,
    ): ApiResponse<Any> {
        adminCouponFacade.deleteCouponTemplate(templateId)
        return ApiResponse.success()
    }

    @GetMapping("/{templateId}/issues")
    override fun getIssuedCoupons(
        @PathVariable templateId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResponse<IssuedCouponInfo>> {
        PageValidator.validatePageRequest(page, size)

        val pageable = PageRequest.of(page, size)
        val couponPage = adminCouponFacade.getIssuedCoupons(templateId, pageable)
        return ApiResponse.success(data = PageResponse.from(couponPage))
    }
}
