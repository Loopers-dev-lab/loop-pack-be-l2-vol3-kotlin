package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderFacade
import com.loopers.domain.user.User
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.auth.AuthenticatedUser
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val orderFacade: OrderFacade,
) : OrderApiSpec {

    @GetMapping
    override fun getOrders(
        @AuthenticatedUser user: User,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startAt: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endAt: LocalDateTime,
    ): ApiResponse<List<OrderDto.GetOrdersResponse>> {
        val orders = orderFacade.getOrders(user.id, startAt, endAt)
        return ApiResponse.success(orders.map { OrderDto.GetOrdersResponse.from(it) })
    }

    @GetMapping("/{orderId}")
    override fun getOrder(
        @AuthenticatedUser user: User,
        @PathVariable orderId: Long,
    ): ApiResponse<OrderDto.GetOrderResponse> {
        val orderDetail = orderFacade.getOrder(user.id, orderId)
        return ApiResponse.success(OrderDto.GetOrderResponse.from(orderDetail))
    }

    @PostMapping
    override fun placeOrder(
        @AuthenticatedUser user: User,
        @RequestBody request: OrderDto.PlaceOrderRequest,
    ): ApiResponse<Unit> {
        orderFacade.placeOrder(user.id, request.toCommands())
        return ApiResponse.success(Unit)
    }
}
