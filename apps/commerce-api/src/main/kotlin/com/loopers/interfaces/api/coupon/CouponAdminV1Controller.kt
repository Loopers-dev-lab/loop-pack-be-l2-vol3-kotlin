package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponFacade
import com.loopers.interfaces.api.ApiResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
    private val couponFacade: CouponFacade,
) : CouponAdminV1ApiSpec {

    @GetMapping
    override fun getTemplates(pageable: Pageable): ApiResponse<Page<CouponAdminV1Dto.CouponTemplateResponse>> =
        couponFacade.getTemplates(pageable)
            .map { CouponAdminV1Dto.CouponTemplateResponse.from(it) }
            .let { ApiResponse.success(it) }

    @GetMapping("/{couponId}")
    override fun getTemplateById(@PathVariable couponId: Long): ApiResponse<CouponAdminV1Dto.CouponTemplateResponse> =
        couponFacade.getTemplateById(couponId)
            .let { CouponAdminV1Dto.CouponTemplateResponse.from(it) }
            .let { ApiResponse.success(it) }

    @PostMapping
    override fun createTemplate(
        @RequestBody request: CouponAdminV1Dto.CreateCouponTemplateRequest,
    ): ApiResponse<CouponAdminV1Dto.CouponTemplateResponse> =
        couponFacade.createTemplate(
            name = request.name,
            type = request.type,
            value = request.value,
            minOrderAmount = request.minOrderAmount,
            expiredAt = request.expiredAt,
        ).let { CouponAdminV1Dto.CouponTemplateResponse.from(it) }
         .let { ApiResponse.success(it) }

    @PutMapping("/{couponId}")
    override fun updateTemplate(
        @PathVariable couponId: Long,
        @RequestBody request: CouponAdminV1Dto.UpdateCouponTemplateRequest,
    ): ApiResponse<CouponAdminV1Dto.CouponTemplateResponse> =
        couponFacade.updateTemplate(
            id = couponId,
            name = request.name,
            type = request.type,
            value = request.value,
            minOrderAmount = request.minOrderAmount,
            expiredAt = request.expiredAt,
        ).let { CouponAdminV1Dto.CouponTemplateResponse.from(it) }
         .let { ApiResponse.success(it) }

    @DeleteMapping("/{couponId}")
    override fun deleteTemplate(@PathVariable couponId: Long): ApiResponse<Unit> =
        couponFacade.deleteTemplate(couponId).let { ApiResponse.success(Unit) }

    @GetMapping("/{couponId}/issues")
    override fun getIssues(
        @PathVariable couponId: Long,
        pageable: Pageable,
    ): ApiResponse<Page<CouponAdminV1Dto.UserCouponIssueResponse>> =
        couponFacade.getIssuesByCouponTemplateId(couponId, pageable)
            .map { CouponAdminV1Dto.UserCouponIssueResponse.from(it) }
            .let { ApiResponse.success(it) }
}
