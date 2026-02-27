package com.loopers.interfaces.admin.order

import com.loopers.application.admin.order.AdminOrderFacade
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/orders")
class AdminOrderV1Controller(
    private val adminOrderFacade: AdminOrderFacade,
) : AdminOrderV1ApiSpec {

    @GetMapping
    override fun getOrders(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResponse<AdminOrderV1Dto.OrderResponse>> {
        if (size !in listOf(20, 50, 100)) {
            throw CoreException(ErrorType.BAD_REQUEST, "size는 20, 50, 100만 가능합니다")
        }
        if (page < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "page는 음수일 수 없습니다")
        }

        val pageable = PageRequest.of(page, size)
        val dto = adminOrderFacade.getOrders(pageable).map { AdminOrderV1Dto.OrderResponse.from(it) }
        val from = PageResponse.from(dto)
        return ApiResponse.success(
            data = from,
        )
    }

    @GetMapping("/{orderId}")
    override fun getOrderById(
        @PathVariable orderId: Long,
    ): ApiResponse<AdminOrderV1Dto.OrderDetailResponse> {
        val order = adminOrderFacade.getOrderById(orderId)
        return ApiResponse.success(AdminOrderV1Dto.OrderDetailResponse.from(order))
    }
}
