package com.loopers.interfaces.api.queue

import com.loopers.application.queue.QueueStrategyType
import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Queue V1 API", description = "주문 대기열 관련 API")
interface QueueV1ApiSpec {
    @Operation(summary = "대기열 진입", description = "선택한 전략의 대기열에 진입합니다.")
    fun enter(
        @Parameter(hidden = true) loginId: String,
        @Parameter(hidden = true) password: String,
        request: QueueV1Dto.EnterRequest,
    ): ApiResponse<QueueV1Dto.PositionResponse>

    @Operation(summary = "대기 순번 조회", description = "현재 순번과 예상 대기 시간을 조회합니다.")
    fun getPosition(
        @Parameter(hidden = true) loginId: String,
        @Parameter(hidden = true) password: String,
        strategy: QueueStrategyType?,
    ): ApiResponse<QueueV1Dto.PositionResponse>
}
