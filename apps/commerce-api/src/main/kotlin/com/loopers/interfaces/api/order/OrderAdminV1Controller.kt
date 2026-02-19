package com.loopers.interfaces.api.order

import com.loopers.domain.order.OrderService
import com.loopers.interfaces.api.order.dto.OrderAdminV1Dto
import com.loopers.interfaces.api.order.spec.OrderAdminV1ApiSpec
import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.toSpringPage
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/orders")
class OrderAdminV1Controller(
    private val orderService: OrderService,
) : OrderAdminV1ApiSpec {

    @GetMapping("/{orderId}")
    override fun getOrder(
        @PathVariable orderId: Long,
    ): ApiResponse<OrderAdminV1Dto.OrderAdminResponse> {
        return orderService.getOrderForAdmin(orderId)
            .let { OrderAdminV1Dto.OrderAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping
    override fun getOrders(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<Page<OrderAdminV1Dto.OrderAdminResponse>> {
        return orderService.getAllOrders(page, size)
            .map { OrderAdminV1Dto.OrderAdminResponse.from(it) }
            .toSpringPage()
            .let { ApiResponse.success(it) }
    }
}
