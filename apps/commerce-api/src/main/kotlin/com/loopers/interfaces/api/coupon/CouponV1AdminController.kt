package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.AdminDeleteCouponUseCase
import com.loopers.application.coupon.AdminGetCouponUseCase
import com.loopers.application.coupon.AdminGetCouponsUseCase
import com.loopers.application.coupon.AdminGetIssuedCouponsUseCase
import com.loopers.application.coupon.AdminModifyCouponUseCase
import com.loopers.application.coupon.AdminRegisterCouponUseCase
import com.loopers.application.coupon.ListCouponsCriteria
import com.loopers.application.coupon.ListIssuedCouponsCriteria
import com.loopers.application.coupon.ModifyCouponCriteria
import com.loopers.application.coupon.RegisterCouponCriteria
import com.loopers.interfaces.api.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/coupons")
class CouponV1AdminController(
    private val adminRegisterCouponUseCase: AdminRegisterCouponUseCase,
    private val adminModifyCouponUseCase: AdminModifyCouponUseCase,
    private val adminDeleteCouponUseCase: AdminDeleteCouponUseCase,
    private val adminGetCouponUseCase: AdminGetCouponUseCase,
    private val adminGetCouponsUseCase: AdminGetCouponsUseCase,
    private val adminGetIssuedCouponsUseCase: AdminGetIssuedCouponsUseCase,
) : CouponV1AdminApiSpec {

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    override fun getCouponTemplates(
        @RequestHeader("X-Loopers-Ldap") ldap: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<CouponV1AdminDto.CouponTemplatesResponse> {
        return adminGetCouponsUseCase.execute(ListCouponsCriteria(page = page, size = size))
            .let { CouponV1AdminDto.CouponTemplatesResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{couponId}")
    @ResponseStatus(HttpStatus.OK)
    override fun getCouponTemplateDetail(
        @RequestHeader("X-Loopers-Ldap") ldap: String,
        @PathVariable couponId: Long,
    ): ApiResponse<CouponV1AdminDto.CouponTemplateDetailResponse> {
        return adminGetCouponUseCase.execute(couponId)
            .let { CouponV1AdminDto.CouponTemplateDetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun register(
        @RequestHeader("X-Loopers-Ldap") ldap: String,
        @RequestBody request: CouponV1AdminDto.RegisterRequest,
    ) {
        adminRegisterCouponUseCase.execute(
            RegisterCouponCriteria(
                name = request.name,
                discountType = request.discountType,
                discountValue = request.discountValue,
                totalQuantity = request.totalQuantity,
                expiredAt = request.expiredAt,
            ),
        )
    }

    @PutMapping("/{couponId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun modify(
        @RequestHeader("X-Loopers-Ldap") ldap: String,
        @PathVariable couponId: Long,
        @RequestBody request: CouponV1AdminDto.ModifyRequest,
    ) {
        adminModifyCouponUseCase.execute(
            ModifyCouponCriteria(
                couponId = couponId,
                name = request.name,
                discountType = request.discountType,
                discountValue = request.discountValue,
                totalQuantity = request.totalQuantity,
                expiredAt = request.expiredAt,
            ),
        )
    }

    @DeleteMapping("/{couponId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun delete(
        @RequestHeader("X-Loopers-Ldap") ldap: String,
        @PathVariable couponId: Long,
    ) {
        adminDeleteCouponUseCase.execute(couponId)
    }

    @GetMapping("/{couponId}/issued")
    @ResponseStatus(HttpStatus.OK)
    override fun getIssuedCoupons(
        @RequestHeader("X-Loopers-Ldap") ldap: String,
        @PathVariable couponId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<CouponV1AdminDto.IssuedCouponsResponse> {
        return adminGetIssuedCouponsUseCase.execute(
            ListIssuedCouponsCriteria(couponId = couponId, page = page, size = size),
        )
            .let { CouponV1AdminDto.IssuedCouponsResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
