package com.loopers.interfaces.api.admin.order

import com.loopers.application.order.AdminOrderFacade
import com.loopers.domain.common.PageResult
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.config.auth.AdminAuthenticated
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
    ): ApiResponse<PageResult<AdminOrderV1Dto.OrderResponse>> {
        val result = adminOrderFacade.getOrders(page, size)
        return PageResult(
            content = result.content.map { AdminOrderV1Dto.OrderResponse.from(it) },
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        ).let { ApiResponse.success(it) }
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
