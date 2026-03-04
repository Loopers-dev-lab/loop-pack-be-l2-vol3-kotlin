package com.loopers.interfaces.api.admin.v1.coupon

import com.loopers.application.coupon.DeleteCouponUseCase
import com.loopers.application.coupon.GetCouponListUseCase
import com.loopers.application.coupon.GetCouponUseCase
import com.loopers.application.coupon.GetUserCouponListUseCase
import com.loopers.application.coupon.RegisterCouponUseCase
import com.loopers.application.coupon.UpdateCouponUseCase
import com.loopers.interfaces.api.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/coupons")
class AdminCouponController(
    private val registerCouponUseCase: RegisterCouponUseCase,
    private val getCouponUseCase: GetCouponUseCase,
    private val getCouponListUseCase: GetCouponListUseCase,
    private val updateCouponUseCase: UpdateCouponUseCase,
    private val deleteCouponUseCase: DeleteCouponUseCase,
    private val getUserCouponListUseCase: GetUserCouponListUseCase,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: CreateCouponRequest,
    ): ApiResponse<CreateCouponResponse> {
        val id = registerCouponUseCase.register(request.toCommand())
        return ApiResponse.success(CreateCouponResponse(id))
    }

    @GetMapping
    fun getAll(): ApiResponse<List<AdminCouponResponse>> {
        val coupons = getCouponListUseCase.getAll()
        return ApiResponse.success(coupons.map { AdminCouponResponse.from(it) })
    }

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: Long,
    ): ApiResponse<AdminCouponResponse> {
        val couponInfo = getCouponUseCase.getById(id)
        return ApiResponse.success(AdminCouponResponse.from(couponInfo))
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateCouponRequest,
    ): ApiResponse<AdminCouponResponse> {
        val couponInfo = updateCouponUseCase.update(id, request.toCommand())
        return ApiResponse.success(AdminCouponResponse.from(couponInfo))
    }

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: Long,
    ): ApiResponse<Nothing?> {
        deleteCouponUseCase.delete(id)
        return ApiResponse.success(null)
    }

    @GetMapping("/{id}/issued")
    fun getIssuedCoupons(
        @PathVariable id: Long,
    ): ApiResponse<List<AdminUserCouponResponse>> {
        val userCoupons = getUserCouponListUseCase.getAllByCouponId(id)
        return ApiResponse.success(userCoupons.map { AdminUserCouponResponse.from(it) })
    }
}
