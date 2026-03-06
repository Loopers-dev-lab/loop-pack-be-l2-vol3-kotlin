package com.loopers.interfaces.api.order

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Order V1 API", description = "주문 API")
interface OrderV1ApiSpec {
    @Operation(summary = "주문 생성", description = "상품을 주문합니다.")
    fun createOrder(loginId: String, password: String, req: OrderV1Dto.CreateOrderRequest): ApiResponse<OrderV1Dto.OrderResponse>

    @Operation(summary = "주문 목록 조회", description = "내 주문 목록을 조회합니다.")
    fun getOrders(loginId: String, password: String, startAt: String, endAt: String): ApiResponse<List<OrderV1Dto.OrderResponse>>

    @Operation(summary = "주문 상세 조회", description = "주문 상세 정보를 조회합니다.")
    fun getOrder(loginId: String, password: String, orderId: Long): ApiResponse<OrderV1Dto.OrderResponse>
}
