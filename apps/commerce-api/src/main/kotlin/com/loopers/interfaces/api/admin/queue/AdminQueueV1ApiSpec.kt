package com.loopers.interfaces.api.admin.queue

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Admin Queue V1 API", description = "어드민 대기열 관리 API 입니다.")
interface AdminQueueV1ApiSpec {

    @Operation(summary = "대기열 토글", description = "대기열을 활성화/비활성화합니다.")
    fun toggleQueue(request: AdminQueueV1Dto.ToggleRequest): ApiResponse<AdminQueueV1Dto.ToggleResponse>

    @Operation(summary = "대기열 상태 조회", description = "대기열 상태를 조회합니다.")
    fun getQueueStatus(): ApiResponse<AdminQueueV1Dto.StatusResponse>
}
