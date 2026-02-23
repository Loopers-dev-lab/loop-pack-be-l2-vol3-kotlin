package com.loopers.interfaces.api.order

import com.loopers.application.order.GetOrderUseCase
import com.loopers.application.order.GetOrdersUseCase
import com.loopers.application.order.PlaceOrderUseCase
import com.loopers.interfaces.api.order.dto.OrderV1Dto
import com.loopers.interfaces.api.order.spec.OrderV1ApiSpec
import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.DateTimeRange
import com.loopers.interfaces.support.auth.AuthUser
import com.loopers.interfaces.support.toSpringPage
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/orders")
class OrderV1Controller(
    private val placeOrderUseCase: PlaceOrderUseCase,
    private val getOrderUseCase: GetOrderUseCase,
    private val getOrdersUseCase: GetOrdersUseCase,
) : OrderV1ApiSpec {

    @PostMapping
    override fun createOrder(
        @AuthUser userId: Long,
        @RequestBody request: OrderV1Dto.CreateOrderRequest,
    ): ApiResponse<OrderV1Dto.OrderResponse> {
        return placeOrderUseCase.execute(userId, request.toCommand())
            .let { OrderV1Dto.OrderResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{orderId}")
    override fun getOrder(
        @AuthUser userId: Long,
        @PathVariable orderId: Long,
    ): ApiResponse<OrderV1Dto.OrderResponse> {
        return getOrderUseCase.execute(userId, orderId)
            .let { OrderV1Dto.OrderResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping
    override fun getOrders(
        @AuthUser userId: Long,
        @RequestParam(required = false) from: String?,
        @RequestParam(required = false) to: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<Page<OrderV1Dto.OrderResponse>> {
        val range = DateTimeRange.of(from, to)
        return getOrdersUseCase.execute(userId, range.from, range.to, page, size)
            .map { OrderV1Dto.OrderResponse.from(it) }
            .toSpringPage()
            .let { ApiResponse.success(it) }
    }
}
