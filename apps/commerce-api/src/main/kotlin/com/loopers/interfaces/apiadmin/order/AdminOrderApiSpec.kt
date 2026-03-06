package com.loopers.interfaces.apiadmin.order

import com.loopers.interfaces.common.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Admin Order API", description = "어드민 주문 API")
interface AdminOrderApiSpec {
    @Operation(
        summary = "주문 목록 조회",
        description = "모든 유저의 주문 목록을 페이징하여 조회합니다.",
    )
    fun getOrders(
        page: Int,
        size: Int,
    ): ApiResponse<AdminOrderDto.PageResponse>

    @Operation(
        summary = "주문 상세 조회",
        description = "주문 ID로 주문 상세 정보를 조회합니다.",
    )
    fun getOrder(orderId: Long): ApiResponse<AdminOrderDto.DetailResponse>

    @Operation(
        summary = "주문 상태 변경",
        description = "주문의 상태를 변경합니다.",
    )
    fun changeOrderStatus(orderId: Long, request: AdminOrderDto.ChangeStatusRequest): ApiResponse<Unit>
}
