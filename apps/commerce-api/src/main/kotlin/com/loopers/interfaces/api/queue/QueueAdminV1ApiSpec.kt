package com.loopers.interfaces.api.queue

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Queue Admin", description = "대기열 관리 API (어드민 전용)")
interface QueueAdminV1ApiSpec {

    @Operation(summary = "대기열 활성화", description = "대기열을 활성화한다. 이후 주문 API 호출 시 입장 토큰이 필수가 된다.")
    fun enableQueue(): ApiResponse<QueueAdminV1Dto.QueueStatusResponse>

    @Operation(summary = "대기열 비활성화", description = "대기열을 비활성화한다. 이후 주문 API 호출 시 토큰 없이 통과된다.")
    fun disableQueue(): ApiResponse<QueueAdminV1Dto.QueueStatusResponse>

    @Operation(summary = "대기열 상태 조회", description = "현재 대기열 활성화 여부를 조회한다.")
    fun getQueueStatus(): ApiResponse<QueueAdminV1Dto.QueueStatusResponse>
}
