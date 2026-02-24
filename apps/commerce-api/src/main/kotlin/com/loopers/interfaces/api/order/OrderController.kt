package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderFacade
import com.loopers.domain.user.User
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.auth.AuthenticatedUser
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val orderFacade: OrderFacade,
) : OrderApiSpec {

    @PostMapping
    override fun placeOrder(
        @AuthenticatedUser user: User,
        @RequestBody request: OrderDto.PlaceOrderRequest,
    ): ApiResponse<Unit> {
        orderFacade.placeOrder(user.id, request.toCommands())
        return ApiResponse.success(Unit)
    }
}
