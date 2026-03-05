package com.loopers.interfaces.api.order

import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.auth.LoginUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@Tag(name = "Order V1 API", description = "주문 API (대고객)")
interface OrderV1ApiSpec {

    @Operation(
        summary = "주문 생성",
        description = "상품을 주문합니다. 쿠폰 적용 시 couponIssueId를 함께 전달합니다. 주문 1건당 쿠폰 1장만 적용 가능합니다.",
    )
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "주문 성공"),
            SwaggerResponse(responseCode = "400", description = "주문 불가 상품, 재고 부족, 또는 사용 불가 쿠폰"),
            SwaggerResponse(responseCode = "403", description = "본인의 쿠폰이 아님"),
            SwaggerResponse(responseCode = "404", description = "존재하지 않는 상품 또는 쿠폰"),
        ],
    )
    fun createOrder(
        loginUser: LoginUser,
        request: OrderV1Dto.CreateOrderRequest,
    ): ApiResponse<OrderV1Dto.OrderResponse>

    @Operation(summary = "주문 목록 조회", description = "기간별 주문 목록을 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "조회 성공"),
        ],
    )
    fun getOrders(
        loginUser: LoginUser,
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
        loginUser: LoginUser,
        orderId: Long,
    ): ApiResponse<OrderV1Dto.OrderResponse>
}
