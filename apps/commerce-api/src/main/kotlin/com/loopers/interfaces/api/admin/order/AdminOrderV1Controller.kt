package com.loopers.interfaces.api.admin.order

import com.loopers.application.order.AdminOrderFacade
import com.loopers.config.auth.AdminAuthenticated
import com.loopers.interfaces.api.ApiResponse
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@AdminAuthenticated
@RestController
@RequestMapping("/api-admin/v1/orders")
class AdminOrderV1Controller(
    private val adminOrderFacade: AdminOrderFacade,
) : AdminOrderV1ApiSpec {
    @GetMapping
    override fun getOrders(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<Page<AdminOrderV1Dto.OrderResponse>> {
        return adminOrderFacade.getOrders(page, size)
            .map { AdminOrderV1Dto.OrderResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{orderId}")
    override fun getOrder(
        @PathVariable orderId: Long,
    ): ApiResponse<AdminOrderV1Dto.OrderResponse> {
        return adminOrderFacade.getOrder(orderId)
            .let { AdminOrderV1Dto.OrderResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
