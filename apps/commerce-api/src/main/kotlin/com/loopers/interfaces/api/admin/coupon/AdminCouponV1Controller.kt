package com.loopers.interfaces.api.admin.coupon

import com.loopers.application.admin.coupon.AdminCouponDeleteUseCase
import com.loopers.application.admin.coupon.AdminCouponDetailUseCase
import com.loopers.application.admin.coupon.AdminCouponIssueListUseCase
import com.loopers.application.admin.coupon.AdminCouponListUseCase
import com.loopers.application.admin.coupon.AdminCouponRegisterUseCase
import com.loopers.application.admin.coupon.AdminCouponUpdateUseCase
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api-admin/v1/coupons")
@RestController
class AdminCouponV1Controller(
    private val registerUseCase: AdminCouponRegisterUseCase,
    private val updateUseCase: AdminCouponUpdateUseCase,
    private val deleteUseCase: AdminCouponDeleteUseCase,
    private val detailUseCase: AdminCouponDetailUseCase,
    private val listUseCase: AdminCouponListUseCase,
    private val issueListUseCase: AdminCouponIssueListUseCase,
) : AdminCouponV1ApiSpec {

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    override fun register(
        @RequestHeader("X-Loopers-Ldap") ldap: String,
        @Valid @RequestBody request: AdminCouponV1Request.Register,
    ): ApiResponse<AdminCouponV1Response.Register> {
        validateLdap(ldap)
        return registerUseCase.register(request.toCommand(ldap))
            .let { AdminCouponV1Response.Register.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping
    override fun getList(
        @RequestHeader("X-Loopers-Ldap") ldap: String,
        pageRequest: PageRequest,
    ): ApiResponse<PageResponse<AdminCouponV1Response.Summary>> {
        validateLdap(ldap)
        return listUseCase.getList(pageRequest)
            .map { AdminCouponV1Response.Summary.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{couponId}")
    override fun getDetail(
        @RequestHeader("X-Loopers-Ldap") ldap: String,
        @PathVariable couponId: Long,
    ): ApiResponse<AdminCouponV1Response.Detail> {
        validateLdap(ldap)
        return detailUseCase.getDetail(couponId)
            .let { AdminCouponV1Response.Detail.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/{couponId}")
    override fun update(
        @RequestHeader("X-Loopers-Ldap") ldap: String,
        @PathVariable couponId: Long,
        @Valid @RequestBody request: AdminCouponV1Request.Update,
    ): ApiResponse<AdminCouponV1Response.Update> {
        validateLdap(ldap)
        return updateUseCase.update(request.toCommand(couponId, ldap))
            .let { AdminCouponV1Response.Update.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{couponId}")
    override fun delete(
        @RequestHeader("X-Loopers-Ldap") ldap: String,
        @PathVariable couponId: Long,
    ): ApiResponse<Any> {
        validateLdap(ldap)
        deleteUseCase.delete(couponId)
        return ApiResponse.success()
    }

    @GetMapping("/{couponId}/issues")
    override fun getIssueList(
        @RequestHeader("X-Loopers-Ldap") ldap: String,
        @PathVariable couponId: Long,
        pageRequest: PageRequest,
    ): ApiResponse<PageResponse<AdminCouponV1Response.IssuedCouponItem>> {
        validateLdap(ldap)
        return issueListUseCase.getIssueList(couponId, pageRequest)
            .map { AdminCouponV1Response.IssuedCouponItem.from(it) }
            .let { ApiResponse.success(it) }
    }

    private fun validateLdap(ldap: String) {
        if (!LDAP_PATTERN.matches(ldap)) {
            throw CoreException(ErrorType.UNAUTHORIZED)
        }
    }

    companion object {
        private val LDAP_PATTERN = Regex("^loopers\\.[a-z]{1,12}$")
    }
}
