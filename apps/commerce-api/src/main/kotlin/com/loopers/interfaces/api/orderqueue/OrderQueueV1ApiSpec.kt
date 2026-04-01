package com.loopers.interfaces.api.orderqueue

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@Tag(name = "Order Queue V1 API", description = "주문 대기열 관련 사용자 API 입니다.")
interface OrderQueueV1ApiSpec {
    @Operation(
        summary = "대기열 진입",
        description = "주문 대기열에 진입합니다.",
    )
    @SwaggerResponse(responseCode = "200", description = "대기열 진입 성공")
    fun enter(
        loginId: String,
        loginPw: String,
    ): ApiResponse<OrderQueueV1Dto.EnterResponse>

    @Operation(
        summary = "대기열 순번 조회",
        description = "현재 대기열 순번 및 토큰 상태를 조회합니다.",
    )
    @SwaggerResponse(responseCode = "200", description = "조회 성공")
    fun getPosition(
        loginId: String,
        loginPw: String,
    ): ApiResponse<OrderQueueV1Dto.PositionResponse>
}
