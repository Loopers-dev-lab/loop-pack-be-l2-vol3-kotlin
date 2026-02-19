package com.loopers.interfaces.api.like

import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.auth.AuthUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Like V1 API", description = "좋아요 API")
interface LikeV1ApiSpec {

    @Operation(summary = "좋아요 등록", description = "상품에 좋아요를 등록합니다.")
    fun addLike(
        @Parameter(hidden = true) @AuthUser userId: Long,
        productId: Long,
    ): ApiResponse<Any>

    @Operation(summary = "좋아요 취소", description = "상품 좋아요를 취소합니다.")
    fun removeLike(
        @Parameter(hidden = true) @AuthUser userId: Long,
        productId: Long,
    ): ApiResponse<Any>

    @Operation(summary = "좋아요 목록 조회", description = "내 좋아요 목록을 조회합니다.")
    fun getLikes(
        @Parameter(hidden = true) @AuthUser userId: Long,
    ): ApiResponse<List<LikeV1Dto.LikeResponse>>
}
