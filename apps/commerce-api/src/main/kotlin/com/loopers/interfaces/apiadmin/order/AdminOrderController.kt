package com.loopers.interfaces.apiadmin.order

import com.loopers.application.order.AdminOrderFacade
import com.loopers.support.common.PageQuery
import com.loopers.support.common.SortOrder
import com.loopers.interfaces.common.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/orders")
class AdminOrderController(
    private val adminOrderFacade: AdminOrderFacade,
) : AdminOrderApiSpec {

    @GetMapping
    override fun getOrders(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<AdminOrderDto.PageResponse> {
        val pageQuery = PageQuery(page, size, SortOrder.UNSORTED)
        return adminOrderFacade.getOrders(pageQuery)
            .let { AdminOrderDto.PageResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{orderId}")
    override fun getOrder(
        @PathVariable orderId: Long,
    ): ApiResponse<AdminOrderDto.DetailResponse> {
        return adminOrderFacade.getOrder(orderId)
            .let { AdminOrderDto.DetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PatchMapping("/{orderId}/status")
    override fun changeOrderStatus(
        @PathVariable orderId: Long,
        @RequestBody request: AdminOrderDto.ChangeStatusRequest,
    ): ApiResponse<Unit> {
        adminOrderFacade.changeOrderStatus(orderId, request.status)
        return ApiResponse.success(Unit)
    }
}
