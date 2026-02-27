package com.loopers.interfaces.api.order

import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.PageResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Order V1 API", description = "주문 관련 API")
interface OrderV1ApiSpec {

    @Operation(summary = "주문 생성", description = "새 주문을 생성합니다.")
    fun createOrder(userId: Long, request: OrderCreateRequest): ApiResponse<OrderCreateResponse>

    @Operation(summary = "내 주문 목록 조회", description = "기간별 내 주문 목록을 조회합니다.")
    fun getMyOrders(
        userId: Long,
        startDate: String,
        endDate: String,
        page: Int,
        size: Int,
    ): ApiResponse<PageResult<OrderSummaryResponse>>

    @Operation(summary = "내 주문 상세 조회", description = "내 주문의 상세 정보를 조회합니다.")
    fun getMyOrder(userId: Long, orderId: Long): ApiResponse<OrderDetailResponse>
}
