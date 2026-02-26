package com.loopers.interfaces.api.like

import com.loopers.interfaces.config.auth.AuthenticatedMember
import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Like V1 API", description = "좋아요 API 입니다.")
interface LikeV1ApiSpec {
    @Operation(summary = "좋아요 등록", description = "상품에 좋아요를 등록합니다.")
    fun like(authenticatedMember: AuthenticatedMember, productId: Long): ApiResponse<Any>

    @Operation(summary = "좋아요 취소", description = "상품의 좋아요를 취소합니다.")
    fun unlike(authenticatedMember: AuthenticatedMember, productId: Long): ApiResponse<Any>

    @Operation(summary = "내 좋아요 목록 조회", description = "내 좋아요 목록을 조회합니다.")
    fun getMyLikes(authenticatedMember: AuthenticatedMember): ApiResponse<List<LikeV1Dto.LikedProductResponse>>
}
