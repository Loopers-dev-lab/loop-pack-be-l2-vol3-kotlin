package com.loopers.interfaces.api.like

import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.PageResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Like V1 API", description = "좋아요 관련 API")
interface LikeV1ApiSpec {

    @Operation(summary = "좋아요 등록", description = "상품에 좋아요를 등록합니다.")
    fun addLike(userId: Long, request: LikeAddRequest): ApiResponse<Unit>

    @Operation(summary = "좋아요 취소", description = "상품의 좋아요를 취소합니다.")
    fun cancelLike(userId: Long, productId: Long): ApiResponse<Unit>

    @Operation(summary = "내 좋아요 목록 조회", description = "현재 사용자의 좋아요 목록을 조회합니다.")
    fun getMyLikes(userId: Long, page: Int, size: Int): ApiResponse<PageResult<LikeProductResponse>>
}
