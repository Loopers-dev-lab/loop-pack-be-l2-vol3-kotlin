package com.loopers.interfaces.api.like

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Product Like V1 API", description = "상품 좋아요 관련 사용자 API 입니다.")
interface ProductLikeV1ApiSpec {
    @Operation(
        summary = "상품 좋아요 등록",
        description = "상품에 좋아요를 등록합니다.",
    )
    fun likeProduct(
        loginId: String,
        loginPw: String,
        productId: Long,
    )

    @Operation(
        summary = "상품 좋아요 취소",
        description = "상품의 좋아요를 취소합니다.",
    )
    fun unlikeProduct(
        loginId: String,
        loginPw: String,
        productId: Long,
    )

    @Operation(
        summary = "좋아요한 상품 목록 조회",
        description = "사용자가 좋아요한 상품 목록을 페이지네이션으로 조회합니다.",
    )
    fun getLikedProducts(
        loginId: String,
        loginPw: String,
        userId: Long,
        page: Int,
        size: Int,
    ): ApiResponse<ProductLikeV1Dto.LikedProductSliceResponse>
}
