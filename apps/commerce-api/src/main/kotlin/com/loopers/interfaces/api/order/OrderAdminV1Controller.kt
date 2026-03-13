package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderInfo
import com.loopers.domain.order.OrderService
import com.loopers.interfaces.api.ApiResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/orders")
class OrderAdminV1Controller(
    private val orderService: OrderService,
) {
    @GetMapping
    fun findAll(
        @PageableDefault(size = 20) pageable: Pageable,
    ): ApiResponse<Page<OrderInfo>> {
        return ApiResponse.success(
            orderService.findAll(pageable).map { OrderInfo.from(it) },
        )
    }

    @GetMapping("/{orderId}")
    fun findById(@PathVariable orderId: Long): ApiResponse<OrderInfo> {
        return ApiResponse.success(OrderInfo.from(orderService.findById(orderId)))
    }
}
