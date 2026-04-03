package com.loopers.interfaces.api.queue

import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.config.auth.AuthenticatedMember
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Queue V1 API", description = "대기열 API 입니다.")
interface QueueV1ApiSpec {

    @Operation(summary = "대기열 진입", description = "대기열에 진입합니다.")
    fun enterQueue(authenticatedMember: AuthenticatedMember): ApiResponse<QueueV1Dto.EnterResponse>

    @Operation(summary = "순번 조회", description = "대기열 순번과 예상 대기 시간을 조회합니다.")
    fun getPosition(authenticatedMember: AuthenticatedMember): ApiResponse<QueueV1Dto.PositionResponse>
}
