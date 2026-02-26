package com.loopers.interfaces.api.order

import com.loopers.interfaces.config.auth.AuthenticatedMember
import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Order V1 API", description = "주문 API 입니다.")
interface OrderV1ApiSpec {
    @Operation(summary = "주문 생성", description = "새로운 주문을 생성합니다.")
    fun createOrder(
        authenticatedMember: AuthenticatedMember,
        request: OrderV1Dto.CreateRequest,
    ): ApiResponse<OrderV1Dto.OrderResponse>

    @Operation(summary = "주문 목록 조회", description = "내 주문 목록을 조회합니다.")
    fun getOrders(
        authenticatedMember: AuthenticatedMember,
        startAt: String,
        endAt: String,
    ): ApiResponse<List<OrderV1Dto.OrderResponse>>

    @Operation(summary = "주문 상세 조회", description = "주문 상세 정보를 조회합니다.")
    fun getOrder(authenticatedMember: AuthenticatedMember, orderId: Long): ApiResponse<OrderV1Dto.OrderResponse>
}
