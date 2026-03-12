package com.loopers.interfaces.api.admin.coupon

import com.loopers.application.coupon.DeleteCouponUseCase
import com.loopers.application.coupon.GetCouponIssuesUseCase
import com.loopers.application.coupon.GetCouponListUseCase
import com.loopers.application.coupon.GetCouponUseCase
import com.loopers.application.coupon.RegisterCouponUseCase
import com.loopers.application.coupon.UpdateCouponUseCase
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AdminAuth
import com.loopers.support.PageResult
import com.loopers.support.constant.ApiPaths
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

@RestController
@RequestMapping(ApiPaths.AdminCoupons.BASE)
class AdminCouponV1Controller(
    private val registerCouponUseCase: RegisterCouponUseCase,
    private val updateCouponUseCase: UpdateCouponUseCase,
    private val deleteCouponUseCase: DeleteCouponUseCase,
    private val getCouponUseCase: GetCouponUseCase,
    private val getCouponListUseCase: GetCouponListUseCase,
    private val getCouponIssuesUseCase: GetCouponIssuesUseCase,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @AdminAuth adminAuth: Unit,
        @Valid @RequestBody request: AdminCouponRegisterRequest,
    ): ApiResponse<AdminCouponResponse> {
        val couponInfo = registerCouponUseCase.execute(request.toCommand())
        return ApiResponse.success(AdminCouponResponse.from(couponInfo))
    }

    @GetMapping
    fun getCouponList(
        @AdminAuth adminAuth: Unit,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResult<AdminCouponResponse>> {
        val result = getCouponListUseCase.execute(page, size)
        val response = PageResult.of(
            content = result.content.map { AdminCouponResponse.from(it) },
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
        )
        return ApiResponse.success(response)
    }

    @GetMapping("/{couponId}")
    fun getCoupon(
        @AdminAuth adminAuth: Unit,
        @PathVariable couponId: Long,
    ): ApiResponse<AdminCouponResponse> {
        val couponInfo = getCouponUseCase.execute(couponId)
        return ApiResponse.success(AdminCouponResponse.from(couponInfo))
    }

    @PutMapping("/{couponId}")
    fun update(
        @AdminAuth adminAuth: Unit,
        @PathVariable couponId: Long,
        @Valid @RequestBody request: AdminCouponUpdateRequest,
    ): ApiResponse<AdminCouponResponse> {
        val couponInfo = updateCouponUseCase.execute(request.toCommand(couponId))
        return ApiResponse.success(AdminCouponResponse.from(couponInfo))
    }

    @DeleteMapping("/{couponId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @AdminAuth adminAuth: Unit,
        @PathVariable couponId: Long,
    ): ApiResponse<Unit> {
        deleteCouponUseCase.execute(couponId)
        return ApiResponse.success(Unit)
    }

    @GetMapping("/{couponId}/issues")
    fun getCouponIssues(
        @AdminAuth adminAuth: Unit,
        @PathVariable couponId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResult<AdminUserCouponResponse>> {
        val result = getCouponIssuesUseCase.execute(couponId, page, size)
        val response = PageResult.of(
            content = result.content.map { AdminUserCouponResponse.from(it) },
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
        )
        return ApiResponse.success(response)
    }
}
