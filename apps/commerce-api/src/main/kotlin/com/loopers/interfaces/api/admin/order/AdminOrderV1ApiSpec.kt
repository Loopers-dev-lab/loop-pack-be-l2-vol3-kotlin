package com.loopers.interfaces.api.admin.order

import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Admin Order V1 API", description = "어드민 주문 API")
interface AdminOrderV1ApiSpec {
    @Operation(summary = "주문 목록 조회", description = "전체 주문 목록을 페이지네이션하여 조회합니다.")
    fun getOrders(page: Int, size: Int): ApiResponse<PageResponse<AdminOrderV1Dto.OrderResponse>>

    @Operation(summary = "주문 상세 조회", description = "주문 ID로 주문 상세 정보를 조회합니다.")
    fun getOrder(orderId: Long): ApiResponse<AdminOrderV1Dto.OrderResponse>
}
