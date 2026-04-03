package com.loopers.interfaces.api.queue

import com.loopers.interfaces.common.ApiResponse
import com.loopers.support.auth.AuthenticatedUserInfo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Queue API", description = "대기열 API")
interface QueueApiSpec {

    @Operation(
        summary = "대기열 진입",
        description = "대기열에 진입하여 순번을 받습니다.",
    )
    fun enterQueue(userInfo: AuthenticatedUserInfo): ApiResponse<QueueDto.EnterQueueResponse>

    @Operation(
        summary = "순번 조회",
        description = "현재 대기 순번과 예상 시간, 입장 토큰을 조회합니다.",
    )
    fun getPosition(userInfo: AuthenticatedUserInfo): ApiResponse<QueueDto.QueuePositionResponse>
}
