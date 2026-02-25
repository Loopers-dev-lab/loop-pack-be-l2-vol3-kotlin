package com.loopers.interfaces.api.order

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Order V1 API", description = "주문 관련 API")
interface OrderV1ApiSpec {

    @Operation(summary = "주문 생성", description = "새로운 주문을 생성합니다.")
    fun createOrder(
        @Parameter(hidden = true) loginId: String,
        @Parameter(hidden = true) password: String,
        request: OrderV1Dto.CreateRequest,
    ): ApiResponse<OrderV1Dto.DetailResponse>

    @Operation(summary = "주문 상세 조회", description = "주문 ID로 상세 정보를 조회합니다.")
    fun getById(
        @Parameter(hidden = true) loginId: String,
        @Parameter(hidden = true) password: String,
        id: Long,
    ): ApiResponse<OrderV1Dto.DetailResponse>

    @Operation(summary = "내 주문 목록 조회", description = "내 주문 목록을 조회합니다.")
    fun getMyOrders(
        @Parameter(hidden = true) loginId: String,
        @Parameter(hidden = true) password: String,
    ): ApiResponse<List<OrderV1Dto.MainResponse>>

    @Operation(summary = "주문 취소", description = "주문을 취소합니다.")
    fun cancel(
        @Parameter(hidden = true) loginId: String,
        @Parameter(hidden = true) password: String,
        id: Long,
    ): ApiResponse<Any>
}
