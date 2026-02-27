package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderFacade
import com.loopers.interfaces.api.ApiResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/orders")
class OrderAdminV1Controller(
    private val orderFacade: OrderFacade,
) : OrderAdminV1ApiSpec {

    @GetMapping
    override fun getOrders(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<Page<OrderAdminV1Dto.OrderAdminResponse>> {
        val pageable = PageRequest.of(page, size)
        return orderFacade.getOrdersForAdmin(pageable)
            .map { OrderAdminV1Dto.OrderAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{orderId}")
    override fun getOrder(
        @PathVariable orderId: Long,
    ): ApiResponse<OrderAdminV1Dto.OrderAdminResponse> {
        return orderFacade.getOrderForAdmin(orderId)
            .let { OrderAdminV1Dto.OrderAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
