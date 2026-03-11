package com.loopers.interfaces.api.order.spec

import com.loopers.interfaces.api.order.dto.OrderV1Dto
import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.auth.AuthUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import org.springframework.data.domain.Page

@Tag(name = "Order V1 API", description = "주문 API")
interface OrderV1ApiSpec {

    @Operation(summary = "주문 생성", description = "상품을 주문합니다.")
    fun createOrder(
        @Parameter(hidden = true) @AuthUser userId: Long,
        @Valid request: OrderV1Dto.CreateOrderRequest,
    ): ApiResponse<OrderV1Dto.OrderResponse>

    @Operation(summary = "주문 상세 조회", description = "내 주문 상세를 조회합니다.")
    fun getOrder(
        @Parameter(hidden = true) @AuthUser userId: Long,
        @Positive orderId: Long,
    ): ApiResponse<OrderV1Dto.OrderResponse>

    @Operation(summary = "주문 목록 조회", description = "내 주문 목록을 조회합니다.")
    fun getOrders(
        @Parameter(hidden = true) @AuthUser userId: Long,
        from: String?,
        to: String?,
        @PositiveOrZero page: Int,
        @Positive @Max(100) size: Int,
    ): ApiResponse<Page<OrderV1Dto.OrderResponse>>
}
