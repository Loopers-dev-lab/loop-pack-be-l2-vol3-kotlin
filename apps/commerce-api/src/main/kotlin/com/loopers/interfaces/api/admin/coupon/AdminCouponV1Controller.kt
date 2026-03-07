package com.loopers.interfaces.api.admin.coupon

import com.loopers.application.coupon.AdminCouponFacade
import com.loopers.application.coupon.CouponCommand
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.coupon.ExpirationPolicy
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import com.loopers.interfaces.config.auth.AdminAuthenticated
import jakarta.validation.Valid
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

@AdminAuthenticated
@RestController
@RequestMapping("/api-admin/v1/coupons")
class AdminCouponV1Controller(
    private val adminCouponFacade: AdminCouponFacade,
) : AdminCouponV1ApiSpec {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun createTemplate(
        @RequestBody @Valid request: AdminCouponV1Dto.CreateRequest,
    ): ApiResponse<AdminCouponV1Dto.TemplateResponse> {
        val command = CouponCommand.CreateTemplate(
            name = request.name,
            type = CouponType.valueOf(request.type),
            value = request.value,
            minOrderAmount = request.minOrderAmount,
            maxDiscountAmount = request.maxDiscountAmount,
            expirationPolicy = ExpirationPolicy.valueOf(request.expirationPolicy),
            expiredAt = request.expiredAt,
            validDays = request.validDays,
        )
        return adminCouponFacade.createTemplate(command)
            .let { AdminCouponV1Dto.TemplateResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping
    override fun getTemplates(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResponse<AdminCouponV1Dto.TemplateResponse>> {
        val result = adminCouponFacade.getTemplates(page, size)
        return PageResponse(
            content = result.content.map { AdminCouponV1Dto.TemplateResponse.from(it) },
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        ).let { ApiResponse.success(it) }
    }

    @GetMapping("/{couponId}")
    override fun getTemplate(
        @PathVariable couponId: Long,
    ): ApiResponse<AdminCouponV1Dto.TemplateResponse> {
        return adminCouponFacade.getTemplate(couponId)
            .let { AdminCouponV1Dto.TemplateResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/{couponId}")
    override fun updateTemplate(
        @PathVariable couponId: Long,
        @RequestBody @Valid request: AdminCouponV1Dto.UpdateRequest,
    ): ApiResponse<AdminCouponV1Dto.TemplateResponse> {
        val command = CouponCommand.UpdateTemplate(
            name = request.name,
            type = CouponType.valueOf(request.type),
            value = request.value,
            minOrderAmount = request.minOrderAmount,
            maxDiscountAmount = request.maxDiscountAmount,
            expirationPolicy = ExpirationPolicy.valueOf(request.expirationPolicy),
            expiredAt = request.expiredAt,
            validDays = request.validDays,
        )
        return adminCouponFacade.updateTemplate(couponId, command)
            .let { AdminCouponV1Dto.TemplateResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{couponId}")
    override fun deleteTemplate(
        @PathVariable couponId: Long,
    ): ApiResponse<Any> {
        adminCouponFacade.deleteTemplate(couponId)
        return ApiResponse.success()
    }

    @GetMapping("/{couponId}/issues")
    override fun getIssuedCouponsByTemplate(
        @PathVariable couponId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResponse<AdminCouponV1Dto.IssuedCouponResponse>> {
        val result = adminCouponFacade.getIssuedCouponsByTemplate(couponId, page, size)
        return PageResponse(
            content = result.content.map { AdminCouponV1Dto.IssuedCouponResponse.from(it) },
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        ).let { ApiResponse.success(it) }
    }
}
