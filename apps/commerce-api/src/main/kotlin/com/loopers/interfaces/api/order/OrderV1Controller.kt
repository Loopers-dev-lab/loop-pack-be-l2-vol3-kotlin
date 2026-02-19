package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderFacade
import com.loopers.domain.order.OrderService
import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.auth.AuthUser
import com.loopers.interfaces.support.DateTimeRange
import com.loopers.interfaces.support.toSpringPage
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/orders")
class OrderV1Controller(
    private val orderFacade: OrderFacade,
    private val orderService: OrderService,
) : OrderV1ApiSpec {

    @PostMapping
    override fun createOrder(
        @AuthUser userId: Long,
        @RequestBody @Valid request: OrderV1Dto.CreateOrderRequest,
    ): ApiResponse<OrderV1Dto.OrderResponse> {
        return orderFacade.createOrder(userId, request.toCommand())
            .let { OrderV1Dto.OrderResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{orderId}")
    override fun getOrder(
        @AuthUser userId: Long,
        @PathVariable orderId: Long,
    ): ApiResponse<OrderV1Dto.OrderResponse> {
        return orderService.getOrder(userId, orderId)
            .let { OrderV1Dto.OrderResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping
    override fun getOrders(
        @AuthUser userId: Long,
        @RequestParam from: String,
        @RequestParam to: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<Page<OrderV1Dto.OrderResponse>> {
        val range = DateTimeRange(from, to)
        return orderService.getOrdersByUserId(userId, range.from, range.to, page, size)
            .map { OrderV1Dto.OrderResponse.from(it) }
            .toSpringPage()
            .let { ApiResponse.success(it) }
    }
}
