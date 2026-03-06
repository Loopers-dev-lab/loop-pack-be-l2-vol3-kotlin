package com.loopers.interfaces.api.admin.order

import com.loopers.application.order.OrderFacade
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/orders")
class AdminOrderV1Controller(
    private val orderFacade: OrderFacade,
) : AdminOrderV1ApiSpec {
    @GetMapping
    override fun getOrders(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResponse<AdminOrderV1Dto.OrderResponse>> {
        return orderFacade.getOrders(PageRequest.of(page, size))
            .map { AdminOrderV1Dto.OrderResponse.from(it) }
            .let { PageResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{orderId}")
    override fun getOrder(@PathVariable orderId: Long): ApiResponse<AdminOrderV1Dto.OrderResponse> {
        return orderFacade.getOrderForAdmin(orderId)
            .let { AdminOrderV1Dto.OrderResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
