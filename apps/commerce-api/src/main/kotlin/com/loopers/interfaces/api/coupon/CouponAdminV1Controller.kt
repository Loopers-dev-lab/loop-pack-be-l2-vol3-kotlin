package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponService
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/coupons")
class CouponAdminV1Controller(
    private val couponService: CouponService,
) : CouponAdminV1ApiSpec {

    @GetMapping
    override fun getAllCoupons(
        @RequestHeader("X-Loopers-Ldap", required = false) ldap: String?,
        pageable: Pageable,
    ): ApiResponse<Page<CouponAdminV1Dto.CouponAdminResponse>> {
        validateAdminAuth(ldap)
        return couponService.getAllCoupons(pageable)
            .map { CouponAdminV1Dto.CouponAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{couponId}")
    override fun getCoupon(
        @RequestHeader("X-Loopers-Ldap", required = false) ldap: String?,
        @PathVariable couponId: Long,
    ): ApiResponse<CouponAdminV1Dto.CouponAdminResponse> {
        validateAdminAuth(ldap)
        return couponService.getCouponInfo(couponId)
            .let { CouponAdminV1Dto.CouponAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PostMapping
    override fun createCoupon(
        @RequestHeader("X-Loopers-Ldap", required = false) ldap: String?,
        @RequestBody request: CouponAdminV1Dto.CreateRequest,
    ): ApiResponse<CouponAdminV1Dto.CouponAdminResponse> {
        validateAdminAuth(ldap)
        return couponService.createCoupon(request.toCriteria())
            .let { CouponAdminV1Dto.CouponAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/{couponId}")
    override fun updateCoupon(
        @RequestHeader("X-Loopers-Ldap", required = false) ldap: String?,
        @PathVariable couponId: Long,
        @RequestBody request: CouponAdminV1Dto.UpdateRequest,
    ): ApiResponse<CouponAdminV1Dto.CouponAdminResponse> {
        validateAdminAuth(ldap)
        return couponService.updateCoupon(couponId, request.toCriteria())
            .let { CouponAdminV1Dto.CouponAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{couponId}")
    override fun deleteCoupon(
        @RequestHeader("X-Loopers-Ldap", required = false) ldap: String?,
        @PathVariable couponId: Long,
    ): ApiResponse<Any> {
        validateAdminAuth(ldap)
        couponService.deleteCoupon(couponId)
        return ApiResponse.success()
    }

    @GetMapping("/{couponId}/issues")
    override fun getIssuedCoupons(
        @RequestHeader("X-Loopers-Ldap", required = false) ldap: String?,
        @PathVariable couponId: Long,
        pageable: Pageable,
    ): ApiResponse<Page<CouponAdminV1Dto.IssuedCouponAdminResponse>> {
        validateAdminAuth(ldap)
        return couponService.getIssuedCoupons(couponId, pageable)
            .map { CouponAdminV1Dto.IssuedCouponAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    private fun validateAdminAuth(ldap: String?) {
        if (ldap == null || ldap != ADMIN_LDAP) {
            throw CoreException(ErrorType.UNAUTHORIZED, "어드민 인증에 실패했습니다.")
        }
    }

    companion object {
        private const val ADMIN_LDAP = "loopers.admin"
    }
}
