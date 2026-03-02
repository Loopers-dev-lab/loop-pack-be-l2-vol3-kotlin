package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderFacade
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AuthenticatedUser
import com.loopers.interfaces.api.auth.RequireAuth
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/orders")
class OrderV1Controller(
    private val orderFacade: OrderFacade,
) : OrderV1ApiSpec {

    @PostMapping
    override fun createOrder(
        @RequireAuth authenticatedUser: AuthenticatedUser,
        @RequestBody request: OrderV1Dto.CreateOrderRequest,
    ): ApiResponse<OrderV1Dto.OrderResponse> =
        orderFacade.createOrder(
            loginId = authenticatedUser.loginId,
            items = request.items.map { OrderFacade.OrderItemRequest(it.productId, it.quantity) },
        ).let { OrderV1Dto.OrderResponse.from(it) }
         .let { ApiResponse.success(it) }

    @GetMapping
    override fun getOrders(
        @RequireAuth authenticatedUser: AuthenticatedUser,
        @RequestParam startAt: LocalDate,
        @RequestParam endAt: LocalDate,
        pageable: Pageable,
    ): ApiResponse<Page<OrderV1Dto.OrderResponse>> =
        orderFacade.getOrders(authenticatedUser.loginId, startAt, endAt, pageable)
            .map { OrderV1Dto.OrderResponse.from(it) }
            .let { ApiResponse.success(it) }

    @GetMapping("/{orderId}")
    override fun getOrderById(
        @RequireAuth authenticatedUser: AuthenticatedUser,
        @PathVariable orderId: Long,
    ): ApiResponse<OrderV1Dto.OrderResponse> =
        orderFacade.getOrderById(authenticatedUser.loginId, orderId)
            .let { OrderV1Dto.OrderResponse.from(it) }
            .let { ApiResponse.success(it) }
}
