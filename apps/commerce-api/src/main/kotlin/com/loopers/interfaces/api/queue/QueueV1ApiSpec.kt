package com.loopers.interfaces.api.queue

import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.auth.LoginUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Queue", description = "주문 대기열 API")
interface QueueV1ApiSpec {

    @Operation(summary = "대기열 진입", description = "주문 대기열에 진입한다. 토큰 여유가 있으면 즉시 발급된다.")
    fun enterQueue(loginUser: LoginUser): ApiResponse<QueueV1Dto.EnterQueueResponse>

    @Operation(summary = "순번 조회", description = "현재 순번과 예상 대기 시간을 조회한다. 토큰 발급 시 응답에 포함된다.")
    fun getPosition(loginUser: LoginUser): ApiResponse<QueueV1Dto.QueuePositionResponse>
}
