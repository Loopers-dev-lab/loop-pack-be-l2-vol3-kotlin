package com.loopers.interfaces.api.queue

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "Queue V1 API", description = "주문 대기열 API")
interface QueueV1ApiSpec {

    @Operation(
        summary = "대기열 진입",
        description = "주문 대기열에 진입합니다. 이미 대기 중이면 현재 순번을, 토큰을 보유 중이면 position=0을 반환합니다.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "대기열 진입 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
        ],
    )
    fun enterQueue(
        @Parameter(description = "로그인 ID", required = true)
        loginId: String,
        @Parameter(description = "비밀번호", required = true)
        password: String,
    ): ApiResponse<QueueV1Dto.EnterResponse>

    @Operation(
        summary = "대기열 순번 조회",
        description = "현재 대기열 순번과 예상 대기 시간을 조회합니다. 순서가 오면 토큰이 포함됩니다.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "순번 조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
        ],
    )
    fun getPosition(
        @Parameter(description = "로그인 ID", required = true)
        loginId: String,
        @Parameter(description = "비밀번호", required = true)
        password: String,
    ): ApiResponse<QueueV1Dto.PositionResponse>
}
