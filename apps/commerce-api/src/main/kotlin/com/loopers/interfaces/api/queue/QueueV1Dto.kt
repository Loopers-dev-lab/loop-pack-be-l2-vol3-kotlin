package com.loopers.interfaces.api.queue

import com.loopers.application.queue.QueueEntryInfo
import com.loopers.application.queue.QueuePositionInfo
import io.swagger.v3.oas.annotations.media.Schema

class QueueV1Dto {

    @Schema(description = "대기열 진입 응답")
    data class EnterResponse(
        @Schema(description = "현재 순번 (0이면 이미 토큰 보유)", example = "128")
        val position: Long,
        @Schema(description = "예상 대기 시간 (초)", example = "1")
        val estimatedWaitSeconds: Long,
        @Schema(description = "전체 대기 인원", example = "500")
        val totalWaiting: Long,
    ) {
        companion object {
            fun from(info: QueueEntryInfo): EnterResponse =
                EnterResponse(
                    position = info.position,
                    estimatedWaitSeconds = info.estimatedWaitSeconds,
                    totalWaiting = info.totalWaiting,
                )
        }
    }

    @Schema(description = "대기열 순번 조회 응답")
    data class PositionResponse(
        @Schema(description = "현재 순번 (0이면 입장 가능)", example = "42")
        val position: Long,
        @Schema(description = "예상 대기 시간 (초)", example = "0")
        val estimatedWaitSeconds: Long,
        @Schema(description = "전체 대기 인원", example = "500")
        val totalWaiting: Long,
        @Schema(description = "입장 토큰 (순서가 왔을 때만 포함)", example = "1:1234567890123456")
        val token: String?,
    ) {
        companion object {
            fun from(info: QueuePositionInfo): PositionResponse =
                PositionResponse(
                    position = info.position,
                    estimatedWaitSeconds = info.estimatedWaitSeconds,
                    totalWaiting = info.totalWaiting,
                    token = info.token,
                )
        }
    }
}
