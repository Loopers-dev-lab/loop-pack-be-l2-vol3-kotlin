package com.loopers.interfaces.api.admin.v1.order

import com.loopers.application.order.GetOrderUseCase
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/orders")
class AdminOrderController(
    private val getOrderUseCase: GetOrderUseCase,
) {
    @GetMapping
    fun getAll(): ApiResponse<List<AdminOrderResponse>> {
        val orders = getOrderUseCase.getAll()
        return ApiResponse.success(orders.map { AdminOrderResponse.from(it) })
    }

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: Long,
    ): ApiResponse<AdminOrderResponse> {
        val orderInfo = getOrderUseCase.getByIdForAdmin(id)
        return ApiResponse.success(AdminOrderResponse.from(orderInfo))
    }
}
