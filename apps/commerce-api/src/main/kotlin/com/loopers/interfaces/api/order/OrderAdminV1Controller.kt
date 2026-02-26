package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderService
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/orders")
class OrderAdminV1Controller(
    private val orderService: OrderService,
) : OrderAdminV1ApiSpec {

    @GetMapping
    override fun getAllOrders(
        @RequestHeader("X-Loopers-Ldap", required = false) ldap: String?,
        pageable: Pageable,
    ): ApiResponse<Page<OrderAdminV1Dto.OrderAdminResponse>> {
        validateAdminAuth(ldap)
        return orderService.getAllOrders(pageable)
            .map { OrderAdminV1Dto.OrderAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{orderId}")
    override fun getOrder(
        @RequestHeader("X-Loopers-Ldap", required = false) ldap: String?,
        @PathVariable orderId: Long,
    ): ApiResponse<OrderAdminV1Dto.OrderAdminResponse> {
        validateAdminAuth(ldap)
        return orderService.getOrder(orderId)
            .let { OrderAdminV1Dto.OrderAdminResponse.from(it) }
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
