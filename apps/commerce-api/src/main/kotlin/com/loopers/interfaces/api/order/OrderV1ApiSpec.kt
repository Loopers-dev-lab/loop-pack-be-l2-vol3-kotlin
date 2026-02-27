package com.loopers.interfaces.api.order

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@Tag(name = "Order V1 API", description = "주문 API (대고객)")
interface OrderV1ApiSpec {

    @Operation(summary = "주문 생성", description = "상품을 주문합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "주문 성공"),
            SwaggerResponse(responseCode = "400", description = "주문 불가 상품 또는 재고 부족"),
            SwaggerResponse(responseCode = "404", description = "존재하지 않는 상품"),
        ],
    )
    fun createOrder(
        loginId: String,
        loginPw: String,
        request: OrderV1Dto.CreateOrderRequest,
    ): ApiResponse<OrderV1Dto.OrderResponse>

    @Operation(summary = "주문 목록 조회", description = "기간별 주문 목록을 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "조회 성공"),
        ],
    )
    fun getOrders(
        loginId: String,
        loginPw: String,
        request: OrderV1Dto.GetOrdersRequest,
    ): ApiResponse<List<OrderV1Dto.OrderResponse>>

    @Operation(summary = "주문 상세 조회", description = "주문 상세 정보를 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "조회 성공"),
            SwaggerResponse(responseCode = "403", description = "접근 권한 없음"),
            SwaggerResponse(responseCode = "404", description = "존재하지 않는 주문"),
        ],
    )
    fun getOrder(
        loginId: String,
        loginPw: String,
        orderId: Long,
    ): ApiResponse<OrderV1Dto.OrderResponse>
}
