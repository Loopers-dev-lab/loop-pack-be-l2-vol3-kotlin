package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderFacade
import com.loopers.interfaces.config.auth.AuthenticatedMember
import com.loopers.interfaces.config.auth.MemberAuthenticated
import com.loopers.application.order.OrderCommand
import com.loopers.interfaces.api.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.ZoneId

@RestController
@RequestMapping("/api/v1/orders")
class OrderV1Controller(
    private val orderFacade: OrderFacade,
) : OrderV1ApiSpec {
    @MemberAuthenticated
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun createOrder(
        authenticatedMember: AuthenticatedMember,
        @RequestBody @Valid request: OrderV1Dto.CreateRequest,
    ): ApiResponse<OrderV1Dto.OrderResponse> {
        val command = OrderCommand.Create(
            items = request.items.map {
                OrderCommand.CreateOrderItem(
                    productId = it.productId,
                    quantity = it.quantity,
                )
            },
        )
        return orderFacade.createOrder(authenticatedMember.id, command)
            .let { OrderV1Dto.OrderResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @MemberAuthenticated
    @GetMapping
    override fun getOrders(
        authenticatedMember: AuthenticatedMember,
        @RequestParam startAt: String,
        @RequestParam endAt: String,
    ): ApiResponse<List<OrderV1Dto.OrderResponse>> {
        val zone = ZoneId.of("Asia/Seoul")
        val startZdt = LocalDate.parse(startAt).atStartOfDay(zone)
        val endZdt = LocalDate.parse(endAt).plusDays(1).atStartOfDay(zone)
        return orderFacade.getOrders(authenticatedMember.id, startZdt, endZdt)
            .map { OrderV1Dto.OrderResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @MemberAuthenticated
    @GetMapping("/{orderId}")
    override fun getOrder(
        authenticatedMember: AuthenticatedMember,
        @PathVariable orderId: Long,
    ): ApiResponse<OrderV1Dto.OrderResponse> {
        return orderFacade.getOrder(authenticatedMember.id, orderId)
            .let { OrderV1Dto.OrderResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
