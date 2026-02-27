package com.loopers.interfaces.api.order

import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Pageable

@Tag(name = "Order V1 API", description = "주문 관련 API")
interface OrderV1ApiSpec {

    @Operation(
        summary = "주문 생성",
        description = "상품 목록으로 주문을 생성합니다.",
        responses = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "주문 생성 성공"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음"),
        ],
    )
    fun createOrder(
        @Parameter(description = "로그인한 사용자 ID", required = true)
        userId: Long,
        @RequestBody(description = "주문 요청 정보", required = true)
        orderRequest: OrderV1Dto.OrderRequest,
    ): ApiResponse<Long>

    @Operation(
        summary = "사용자 주문 목록 조회",
        description = "사용자의 주문 목록을 페이징 처리하여 조회합니다.",
        responses = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "주문 목록 조회 성공"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        ],
    )
    fun getOrders(
        @Parameter(description = "로그인한 사용자 ID", required = true)
        userId: Long,
        pageable: Pageable,
    ): ApiResponse<PageResponse<OrderV1Dto.OrderResponse>>

    @Operation(
        summary = "주문 상세 조회",
        description = "특정 주문의 상세 정보를 조회합니다.",
        responses = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "주문 조회 성공"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음"),
        ],
    )
    fun getOrder(
        @Parameter(description = "로그인한 사용자 ID", required = true)
        userId: Long,
        @Parameter(description = "주문 ID", required = true)
        orderId: Long,
    ): ApiResponse<OrderV1Dto.OrderResponse>
}
