package com.loopers.interfaces.api.like

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Like V1 API", description = "좋아요 관련 API입니다.")
interface LikeV1ApiSpec {

    @Operation(summary = "좋아요 추가", description = "상품에 좋아요를 추가합니다.")
    fun addLike(loginId: String, loginPw: String, productId: Long): ApiResponse<Any>

    @Operation(summary = "좋아요 취소", description = "상품의 좋아요를 취소합니다.")
    fun removeLike(loginId: String, loginPw: String, productId: Long): ApiResponse<Any>

    @Operation(summary = "좋아요한 상품 목록 조회", description = "사용자가 좋아요한 상품 목록을 조회합니다.")
    fun getLikedProducts(loginId: String, loginPw: String, userId: Long): ApiResponse<List<LikeV1Dto.LikedProductResponse>>
}
