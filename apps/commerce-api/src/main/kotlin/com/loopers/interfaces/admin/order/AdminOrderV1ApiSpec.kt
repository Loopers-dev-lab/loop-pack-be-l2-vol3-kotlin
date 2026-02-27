package com.loopers.interfaces.admin.order

import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Admin Order V1 API", description = "관리자 주문 관련 API")
interface AdminOrderV1ApiSpec {

    @Operation(
        summary = "관리자 주문 목록 조회",
        description = "전체 주문 목록을 페이징 처리하여 조회합니다.",
        responses = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "주문 목록 조회 성공"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 페이징 파라미터"),
        ],
    )
    fun getOrders(
        @Parameter(description = "페이지 번호 (0부터 시작)", required = false, example = "0")
        page: Int,
        @Parameter(description = "페이지 크기 (20, 50, 100만 가능)", required = false, example = "20")
        size: Int,
    ): ApiResponse<PageResponse<AdminOrderV1Dto.OrderResponse>>

    @Operation(
        summary = "관리자 주문 상세 조회",
        description = "특정 주문의 상세 정보를 조회합니다.",
        responses = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "주문 조회 성공"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음"),
        ],
    )
    fun getOrderById(
        @Parameter(description = "주문 ID", required = true)
        orderId: Long,
    ): ApiResponse<AdminOrderV1Dto.OrderDetailResponse>
}
