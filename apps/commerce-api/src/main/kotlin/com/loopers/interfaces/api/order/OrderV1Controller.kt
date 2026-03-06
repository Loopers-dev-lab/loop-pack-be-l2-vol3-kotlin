package com.loopers.interfaces.api.order

import com.loopers.application.api.order.OrderFacade
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/orders")
class OrderV1Controller(
    private val orderFacade: OrderFacade,
) : OrderV1ApiSpec {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun createOrder(
        @RequestAttribute("userId") userId: Long,
        @RequestBody @Valid orderRequest: OrderV1Dto.OrderRequest,
    ): ApiResponse<Long> {
        val orderId = orderFacade.createOrder(userId, orderRequest)
        return ApiResponse.success(orderId)
    }

    @GetMapping
    override fun getOrders(
        @RequestAttribute("userId") userId: Long,
        pageable: Pageable,
    ): ApiResponse<PageResponse<OrderV1Dto.OrderResponse>> {
        val pageData = orderFacade.getOrdersByUserId(userId, pageable).map { OrderV1Dto.OrderResponse.from(it) }
        return ApiResponse.success(PageResponse.from(pageData))
    }

    @GetMapping("/{orderId}")
    override fun getOrder(
        @RequestAttribute("userId") userId: Long,
        @PathVariable orderId: Long,
    ): ApiResponse<OrderV1Dto.OrderResponse> {
        val order = orderFacade.getOrderById(userId, orderId)
        return ApiResponse.success(OrderV1Dto.OrderResponse.from(order))
    }
}
