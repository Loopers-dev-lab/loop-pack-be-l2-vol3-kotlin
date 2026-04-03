package com.loopers.interfaces.api.user.queue

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "[User] Queue V1 API", description = "대기열 API 입니다.")
interface UserQueueV1ApiSpec {
    @Operation(
        summary = "대기열 진입",
        description = "대기열에 진입합니다. 이미 토큰이 발급된 경우 토큰 정보를 반환합니다.",
    )
    fun enter(
        loginId: String,
        password: String,
    ): ApiResponse<UserQueueV1Response.Enter>

    @Operation(
        summary = "대기열 순번 조회",
        description = "현재 대기열 순번을 조회합니다. 토큰이 발급된 경우 READY 상태를 반환합니다. 토큰은 POST /enter에서만 획득 가능합니다.",
    )
    fun getPosition(userId: Long): ApiResponse<UserQueueV1Response.Position>
}
