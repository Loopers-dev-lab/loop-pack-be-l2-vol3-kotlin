package com.loopers.interfaces.api.queue

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Queue V1 API", description = "대기열 관련 API")
interface QueueV1ApiSpec {

    @Operation(summary = "대기열 진입", description = "대기열에 진입합니다. 중복 진입은 불가합니다.")
    fun enter(
        @Parameter(description = "대기열 이름", required = true) queueName: String,
        @Parameter(description = "로그인한 사용자 ID", required = true) userId: Long,
    ): ApiResponse<QueueV1Dto.EnterResponse>

    @Operation(summary = "대기 순번 조회", description = "현재 대기 순번과 예상 대기시간을 조회합니다.")
    fun getPosition(
        @Parameter(description = "대기열 이름", required = true) queueName: String,
        @Parameter(description = "로그인한 사용자 ID", required = true) userId: Long,
    ): ApiResponse<QueueV1Dto.PositionResponse>

    @Operation(summary = "대기열 상태 조회", description = "대기열의 전체 상태를 조회합니다.")
    fun getStatus(
        @Parameter(description = "대기열 이름", required = true) queueName: String,
    ): ApiResponse<QueueV1Dto.StatusResponse>
}
