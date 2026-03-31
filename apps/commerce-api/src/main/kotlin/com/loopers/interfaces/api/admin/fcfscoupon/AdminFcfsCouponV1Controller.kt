package com.loopers.interfaces.api.admin.fcfscoupon

import com.loopers.application.fcfscoupon.AdminFcfsCouponFacade
import com.loopers.application.fcfscoupon.FcfsCouponCommand
import com.loopers.domain.coupon.CouponType
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.config.auth.AdminAuthenticated
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
@RequestMapping("/api-admin/v1/fcfs-coupons")
class AdminFcfsCouponV1Controller(
    private val adminFcfsCouponFacade: AdminFcfsCouponFacade,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createTemplate(
        @RequestBody request: AdminFcfsCouponV1Dto.CreateRequest,
    ): ApiResponse<AdminFcfsCouponV1Dto.TemplateResponse> {
        val command = FcfsCouponCommand.CreateTemplate(
            name = request.name,
            description = request.description,
            discountType = CouponType.valueOf(request.discountType),
            discountValue = request.discountValue,
            minOrderAmount = request.minOrderAmount,
            maxDiscountAmount = request.maxDiscountAmount,
            totalQuantity = request.totalQuantity,
            startedAt = request.startedAt,
            endedAt = request.endedAt,
        )
        return adminFcfsCouponFacade.createTemplate(command)
            .let { AdminFcfsCouponV1Dto.TemplateResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping
    fun getTemplates(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<List<AdminFcfsCouponV1Dto.TemplateResponse>> {
        return adminFcfsCouponFacade.getTemplates(page, size)
            .map { AdminFcfsCouponV1Dto.TemplateResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{id}")
    fun getTemplate(
        @PathVariable id: Long,
    ): ApiResponse<AdminFcfsCouponV1Dto.TemplateResponse> {
        return adminFcfsCouponFacade.getTemplate(id)
            .let { AdminFcfsCouponV1Dto.TemplateResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/{id}")
    fun updateTemplate(
        @PathVariable id: Long,
        @RequestBody request: AdminFcfsCouponV1Dto.UpdateRequest,
    ): ApiResponse<AdminFcfsCouponV1Dto.TemplateResponse> {
        val command = FcfsCouponCommand.UpdateTemplate(
            name = request.name,
            description = request.description,
            discountType = CouponType.valueOf(request.discountType),
            discountValue = request.discountValue,
            minOrderAmount = request.minOrderAmount,
            maxDiscountAmount = request.maxDiscountAmount,
            totalQuantity = request.totalQuantity,
            startedAt = request.startedAt,
            endedAt = request.endedAt,
        )
        return adminFcfsCouponFacade.updateTemplate(id, command)
            .let { AdminFcfsCouponV1Dto.TemplateResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTemplate(
        @PathVariable id: Long,
    ) {
        adminFcfsCouponFacade.deleteTemplate(id)
    }
}
