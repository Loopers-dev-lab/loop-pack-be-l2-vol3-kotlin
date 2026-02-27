package com.loopers.interfaces.apiadmin.order

import com.loopers.application.order.AdminOrderFacade
import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.SortOrder
import com.loopers.interfaces.common.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
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
}
