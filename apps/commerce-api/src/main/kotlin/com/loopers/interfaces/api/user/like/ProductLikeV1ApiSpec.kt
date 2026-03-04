package com.loopers.interfaces.api.user.like

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "[User] Product Like V1 API", description = "상품 좋아요 API 입니다.")
interface ProductLikeV1ApiSpec {
    @Operation(summary = "상품 좋아요 등록", description = "상품에 좋아요를 등록합니다.")
    fun register(loginId: String, password: String, productId: Long): ApiResponse<Nothing?>

    @Operation(summary = "상품 좋아요 취소", description = "상품 좋아요를 취소합니다.")
    fun cancel(loginId: String, password: String, productId: Long): ApiResponse<Nothing?>
}
