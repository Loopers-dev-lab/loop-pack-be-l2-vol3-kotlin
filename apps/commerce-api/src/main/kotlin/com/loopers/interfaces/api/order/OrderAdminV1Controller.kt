package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderFacade
import com.loopers.interfaces.api.ApiResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/orders")
class OrderAdminV1Controller(
    private val orderFacade: OrderFacade,
) : OrderAdminV1ApiSpec {

    @GetMapping
    override fun getOrders(pageable: Pageable): ApiResponse<Page<OrderAdminV1Dto.OrderResponse>> =
        orderFacade.getAllOrders(pageable)
            .map { OrderAdminV1Dto.OrderResponse.from(it) }
            .let { ApiResponse.success(it) }

    @GetMapping("/{orderId}")
    override fun getOrderById(@PathVariable orderId: Long): ApiResponse<OrderAdminV1Dto.OrderResponse> =
        orderFacade.getAdminOrderById(orderId)
            .let { OrderAdminV1Dto.OrderResponse.from(it) }
            .let { ApiResponse.success(it) }
}
