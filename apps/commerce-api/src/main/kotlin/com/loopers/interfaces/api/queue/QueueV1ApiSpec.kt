package com.loopers.interfaces.api.queue

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Queue V1 API", description = "대기열 API")
interface QueueV1ApiSpec {
    @Operation(summary = "대기열 진입", description = "주문 대기열에 진입합니다.")
    fun enterQueue(loginId: String, password: String): ApiResponse<QueueV1Dto.EnterResponse>

    @Operation(summary = "대기열 순번 조회", description = "대기열 내 순번과 상태를 조회합니다.")
    fun getPosition(loginId: String, password: String): ApiResponse<QueueV1Dto.PositionResponse>
}
