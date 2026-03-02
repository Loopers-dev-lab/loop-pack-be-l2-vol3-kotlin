package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderFacade
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.auth.CurrentUser
import com.loopers.support.auth.LoginUser
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/orders")
class OrderV1Controller(
    private val orderFacade: OrderFacade,
) : OrderV1ApiSpec {

    @PostMapping
    override fun createOrder(
        @CurrentUser loginUser: LoginUser,
        @RequestBody request: OrderV1Dto.CreateOrderRequest,
    ): ApiResponse<OrderV1Dto.OrderResponse> {
        return orderFacade.createOrder(loginUser.id, request.toCriteria())
            .let { OrderV1Dto.OrderResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping
    override fun getOrders(
        @CurrentUser loginUser: LoginUser,
        @ParameterObject request: OrderV1Dto.GetOrdersRequest,
    ): ApiResponse<List<OrderV1Dto.OrderResponse>> {
        return orderFacade.getOrders(loginUser.id, request.toCriteria())
            .map { OrderV1Dto.OrderResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{orderId}")
    override fun getOrder(
        @CurrentUser loginUser: LoginUser,
        @PathVariable orderId: Long,
    ): ApiResponse<OrderV1Dto.OrderResponse> {
        return orderFacade.getOrder(loginUser.id, orderId)
            .let { OrderV1Dto.OrderResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
