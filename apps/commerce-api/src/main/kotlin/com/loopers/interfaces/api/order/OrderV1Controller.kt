package com.loopers.interfaces.api.order

import com.loopers.application.auth.AuthFacade
import com.loopers.application.order.OrderFacade
import com.loopers.application.order.OrderItemCommand
import com.loopers.application.order.PlaceOrderCommand
import com.loopers.domain.order.OrderService
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.security.AuthHeader
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/orders")
class OrderV1Controller(
    private val authFacade: AuthFacade,
    private val orderFacade: OrderFacade,
    private val orderService: OrderService,
) : OrderV1ApiSpec {

    @PostMapping
    override fun placeOrder(
        @RequestHeader(AuthHeader.HEADER_LOGIN_ID) loginId: String,
        @RequestHeader(AuthHeader.HEADER_LOGIN_PW) loginPw: String,
        @RequestBody request: OrderV1Dto.PlaceOrderRequest,
    ): ApiResponse<OrderV1Dto.OrderResponse> {
        val user = authFacade.authenticate(loginId, loginPw)
        val cmd = PlaceOrderCommand(
            items = request.items.map { OrderItemCommand(productId = it.productId, quantity = it.quantity) }
        )
        return orderFacade.placeOrder(user.id, cmd)
            .let { OrderV1Dto.OrderResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping
    override fun getOrders(
        @RequestHeader(AuthHeader.HEADER_LOGIN_ID) loginId: String,
        @RequestHeader(AuthHeader.HEADER_LOGIN_PW) loginPw: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startAt: LocalDate?,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endAt: LocalDate?,
    ): ApiResponse<List<OrderV1Dto.OrderResponse>> {
        val user = authFacade.authenticate(loginId, loginPw)
        return orderService.getOrders(user.id, startAt, endAt)
            .map { OrderV1Dto.OrderResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{orderId}")
    override fun getOrderDetail(
        @RequestHeader(AuthHeader.HEADER_LOGIN_ID) loginId: String,
        @RequestHeader(AuthHeader.HEADER_LOGIN_PW) loginPw: String,
        @PathVariable orderId: Long,
    ): ApiResponse<OrderV1Dto.OrderResponse> {
        authFacade.authenticate(loginId, loginPw)
        return orderService.getById(orderId)
            .let { OrderV1Dto.OrderResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
