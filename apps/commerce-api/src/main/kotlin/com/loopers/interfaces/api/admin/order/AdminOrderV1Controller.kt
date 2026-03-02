package com.loopers.interfaces.api.admin.order

import com.loopers.application.order.GetAllOrdersUseCase
import com.loopers.application.order.GetOrderDetailUseCase
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AdminAuth
import com.loopers.support.PageResult
import com.loopers.support.constant.ApiPaths
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(ApiPaths.AdminOrders.BASE)
class AdminOrderV1Controller(
    private val getAllOrdersUseCase: GetAllOrdersUseCase,
    private val getOrderDetailUseCase: GetOrderDetailUseCase,
) {

    @GetMapping
    fun getAllOrders(
        @AdminAuth adminAuth: Unit,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResult<AdminOrderSummaryResponse>> {
        val result = getAllOrdersUseCase.execute(page, size)
        val response = PageResult.of(
            content = result.content.map { AdminOrderSummaryResponse.from(it) },
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
        )
        return ApiResponse.success(response)
    }

    @GetMapping("/{orderId}")
    fun getOrderDetail(
        @AdminAuth adminAuth: Unit,
        @PathVariable orderId: Long,
    ): ApiResponse<AdminOrderDetailResponse> {
        val result = getOrderDetailUseCase.execute(orderId)
        return ApiResponse.success(AdminOrderDetailResponse.from(result))
    }
}
