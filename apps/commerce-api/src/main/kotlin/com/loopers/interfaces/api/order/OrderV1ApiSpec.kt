package com.loopers.interfaces.api.order

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@Tag(name = "Order V1 API", description = "주문 관련 사용자 API 입니다.")
interface OrderV1ApiSpec {
    @Operation(
        summary = "주문 생성",
        description = "새로운 주문을 생성합니다.",
    )
    @SwaggerResponse(responseCode = "201", description = "주문 생성 성공")
    fun createOrder(
        loginId: String,
        loginPw: String,
        request: OrderV1Dto.CreateOrderRequest,
    ): ApiResponse<Any>

    @Operation(
        summary = "주문 목록 조회",
        description = "기간별 주문 목록을 조회합니다.",
    )
    @SwaggerResponse(responseCode = "200", description = "조회 성공")
    fun getOrders(
        loginId: String,
        loginPw: String,
        startAt: String,
        endAt: String,
        page: Int,
        size: Int,
    ): ApiResponse<OrderV1Dto.OrderSliceResponse>

    @Operation(
        summary = "주문 상세 조회",
        description = "주문의 상세 정보를 조회합니다.",
    )
    @SwaggerResponse(responseCode = "200", description = "조회 성공")
    fun getOrder(
        loginId: String,
        loginPw: String,
        orderId: Long,
    ): ApiResponse<OrderV1Dto.OrderResponse>
}
