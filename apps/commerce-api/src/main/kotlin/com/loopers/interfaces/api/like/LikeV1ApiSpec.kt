package com.loopers.interfaces.api.like

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Like V1 API", description = "좋아요 관련 API")
interface LikeV1ApiSpec {

    @Operation(summary = "좋아요 등록", description = "상품에 좋아요를 등록합니다.")
    fun register(
        @Parameter(hidden = true) loginId: String,
        @Parameter(hidden = true) password: String,
        request: LikeV1Dto.RegisterRequest,
    ): ApiResponse<LikeV1Dto.RegisteredResponse>

    @Operation(summary = "좋아요 취소", description = "좋아요를 취소합니다.")
    fun remove(
        @Parameter(hidden = true) loginId: String,
        @Parameter(hidden = true) password: String,
        id: Long,
    ): ApiResponse<Any>

    @Operation(summary = "내 좋아요 목록 조회", description = "내가 좋아요한 상품 목록을 조회합니다.")
    fun getMyLikes(
        @Parameter(hidden = true) loginId: String,
        @Parameter(hidden = true) password: String,
    ): ApiResponse<List<LikeV1Dto.DetailResponse>>
}
