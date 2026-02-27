package com.loopers.interfaces.api.order

import com.loopers.application.order.AdminGetOrderUseCase
import com.loopers.application.order.AdminGetOrdersUseCase
import com.loopers.application.order.ListOrdersCriteria
import com.loopers.interfaces.api.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/orders")
class OrderV1AdminController(
    private val adminGetOrdersUseCase: AdminGetOrdersUseCase,
    private val adminGetOrderUseCase: AdminGetOrderUseCase,
) : OrderV1AdminApiSpec {

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    override fun getOrders(
        @RequestHeader("X-Loopers-Ldap") ldap: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<OrderV1AdminDto.OrderSliceResponse> {
        return adminGetOrdersUseCase.execute(ListOrdersCriteria(page = page, size = size))
            .let { OrderV1AdminDto.OrderSliceResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{orderId}")
    @ResponseStatus(HttpStatus.OK)
    override fun getOrder(
        @RequestHeader("X-Loopers-Ldap") ldap: String,
        @PathVariable orderId: Long,
    ): ApiResponse<OrderV1AdminDto.OrderDetailResponse> {
        return adminGetOrderUseCase.execute(orderId)
            .let { OrderV1AdminDto.OrderDetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
