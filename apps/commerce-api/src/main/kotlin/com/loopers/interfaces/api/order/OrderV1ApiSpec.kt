package com.loopers.interfaces.api.order

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import java.time.ZonedDateTime
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "Order V1 API", description = "주문 관련 API")
interface OrderV1ApiSpec {

    @Operation(summary = "주문 생성", description = "상품을 주문합니다. 재고가 부족한 상품은 제외되고 부분 주문이 생성될 수 있습니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "주문 성공"),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 요청 (모든 상품 재고 부족 등)"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
        ],
    )
    fun createOrder(
        @Parameter(description = "로그인 ID", required = true)
        loginId: String,
        @Parameter(description = "비밀번호", required = true)
        password: String,
        request: OrderV1Dto.CreateRequest,
    ): ApiResponse<OrderV1Dto.CreateOrderResponse>

    @Operation(summary = "내 주문 목록 조회", description = "본인의 주문 목록을 기간별로 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
        ],
    )
    fun getUserOrders(
        @Parameter(description = "로그인 ID", required = true)
        loginId: String,
        @Parameter(description = "비밀번호", required = true)
        password: String,
        @Parameter(description = "조회 시작일시", required = true)
        startAt: ZonedDateTime,
        @Parameter(description = "조회 종료일시", required = true)
        endAt: ZonedDateTime,
    ): ApiResponse<List<OrderV1Dto.OrderSummaryResponse>>

    @Operation(summary = "주문 상세 조회", description = "주문 ID로 주문 상세를 조회합니다. 본인의 주문만 조회 가능합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "403", description = "본인의 주문만 조회 가능"),
            SwaggerApiResponse(responseCode = "404", description = "주문 없음"),
        ],
    )
    fun getOrder(
        @Parameter(description = "로그인 ID", required = true)
        loginId: String,
        @Parameter(description = "비밀번호", required = true)
        password: String,
        @Parameter(description = "주문 ID", required = true)
        orderId: Long,
    ): ApiResponse<OrderV1Dto.OrderResponse>
}
