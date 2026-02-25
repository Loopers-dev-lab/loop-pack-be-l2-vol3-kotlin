package com.loopers.interfaces.api.order

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import java.time.LocalDate

@Tag(name = "Order V1 API", description = "주문 관련 API입니다.")
interface OrderV1ApiSpec {

    @Operation(summary = "주문 생성", description = "새로운 주문을 생성합니다.")
    fun placeOrder(loginId: String, loginPw: String, request: OrderV1Dto.PlaceOrderRequest): ApiResponse<OrderV1Dto.OrderResponse>

    @Operation(summary = "주문 목록 조회", description = "사용자의 주문 목록을 조회합니다. startAt과 endAt은 모두 제공하거나 모두 생략해야 합니다.")
    fun getOrders(loginId: String, loginPw: String, startAt: LocalDate?, endAt: LocalDate?): ApiResponse<List<OrderV1Dto.OrderResponse>>

    @Operation(summary = "주문 상세 조회", description = "주문 ID로 주문 상세 정보를 조회합니다.")
    fun getOrderDetail(loginId: String, loginPw: String, orderId: Long): ApiResponse<OrderV1Dto.OrderResponse>
}
