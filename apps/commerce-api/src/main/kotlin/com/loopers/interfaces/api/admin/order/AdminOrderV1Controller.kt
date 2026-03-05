package com.loopers.interfaces.api.admin.order

import com.loopers.application.admin.order.AdminOrderDetailUseCase
import com.loopers.application.admin.order.AdminOrderListUseCase
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RequestMapping("/api-admin/v1/orders")
@RestController
class AdminOrderV1Controller(
    private val listUseCase: AdminOrderListUseCase,
    private val detailUseCase: AdminOrderDetailUseCase,
) : AdminOrderV1ApiSpec {

    @GetMapping
    override fun getList(
        @RequestHeader("X-Loopers-Ldap") ldap: String,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
        pageRequest: PageRequest,
    ): ApiResponse<PageResponse<AdminOrderV1Response.ListItem>> {
        validateLdap(ldap)
        return listUseCase.getList(from, to, pageRequest)
            .map { AdminOrderV1Response.ListItem.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{orderId}")
    override fun getDetail(
        @RequestHeader("X-Loopers-Ldap") ldap: String,
        @PathVariable orderId: Long,
    ): ApiResponse<AdminOrderV1Response.Detail> {
        validateLdap(ldap)
        return detailUseCase.getDetail(orderId)
            .let { AdminOrderV1Response.Detail.from(it) }
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
