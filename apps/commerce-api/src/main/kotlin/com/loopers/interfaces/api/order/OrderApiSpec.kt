package com.loopers.interfaces.api.order

import com.loopers.support.auth.AuthenticatedUserInfo
import com.loopers.interfaces.common.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import java.time.LocalDateTime

@Tag(name = "Order API", description = "주문 API")
interface OrderApiSpec {

    @Operation(
        summary = "주문 요청",
        description = "상품을 주문합니다. 재고가 차감되고 주문이 생성됩니다.",
    )
    fun placeOrder(userInfo: AuthenticatedUserInfo, request: OrderDto.PlaceOrderRequest): ApiResponse<Unit>

    @Operation(
        summary = "주문 목록 조회",
        description = "기간별 주문 목록을 조회합니다.",
    )
    fun getOrders(
        userInfo: AuthenticatedUserInfo,
        startAt: LocalDateTime,
        endAt: LocalDateTime,
    ): ApiResponse<List<OrderDto.GetOrdersResponse>>

    @Operation(
        summary = "주문 상세 조회",
        description = "주문 ID로 주문 상세 정보를 조회합니다.",
    )
    fun getOrder(userInfo: AuthenticatedUserInfo, orderId: Long): ApiResponse<OrderDto.GetOrderResponse>
}
