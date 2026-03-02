package com.loopers.interfaces.api.order

import com.loopers.application.order.GetOrderAdminUseCase
import com.loopers.application.order.GetOrdersAdminUseCase
import com.loopers.interfaces.api.order.dto.OrderAdminV1Dto
import com.loopers.interfaces.api.order.spec.OrderAdminV1ApiSpec
import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.toSpringPage
import org.springframework.data.domain.Page
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api-admin/v1/orders")
class OrderAdminV1Controller(
    private val getOrderAdminUseCase: GetOrderAdminUseCase,
    private val getOrdersAdminUseCase: GetOrdersAdminUseCase,
) : OrderAdminV1ApiSpec {

    @GetMapping("/{orderId}")
    override fun getOrder(
        @PathVariable orderId: Long,
    ): ApiResponse<OrderAdminV1Dto.OrderAdminResponse> {
        return getOrderAdminUseCase.execute(orderId)
            .let { OrderAdminV1Dto.OrderAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping
    override fun getOrders(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<Page<OrderAdminV1Dto.OrderAdminResponse>> {
        return getOrdersAdminUseCase.execute(page, size)
            .map { OrderAdminV1Dto.OrderAdminResponse.from(it) }
            .toSpringPage()
            .let { ApiResponse.success(it) }
    }
}
