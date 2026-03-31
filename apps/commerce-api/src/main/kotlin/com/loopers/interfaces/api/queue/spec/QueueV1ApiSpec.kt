package com.loopers.interfaces.api.queue.spec

import com.loopers.interfaces.api.queue.dto.QueueV1Dto
import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.auth.AuthUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Tag(name = "Queue V1 API", description = "대기열 API")
interface QueueV1ApiSpec {

    @Operation(summary = "대기열 진입", description = "대기열에 진입합니다. 이미 토큰이 있으면 토큰 정보를 반환합니다.")
    fun enterQueue(
        @Parameter(hidden = true) @AuthUser userId: Long,
    ): ApiResponse<QueueV1Dto.QueuePositionResponse>

    @Operation(summary = "대기열 순번 조회", description = "현재 대기열 순번을 조회합니다. 토큰이 발급되었으면 토큰 정보를 포함합니다.")
    fun getQueuePosition(
        @Parameter(hidden = true) @AuthUser userId: Long,
    ): ApiResponse<QueueV1Dto.QueuePositionResponse>

    @Operation(
        summary = "대기열 실시간 이벤트 스트림",
        description = "SSE로 순번 변경 및 토큰 발급 이벤트를 실시간 수신합니다. " +
            "이벤트 타입: position(순번 갱신), token-issued(토큰 발급).",
    )
    fun streamQueueEvents(
        @Parameter(hidden = true) @AuthUser userId: Long,
    ): SseEmitter
}
