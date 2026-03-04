package com.loopers.interfaces.api.user.like

import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "[User] Like List V1 API", description = "좋아요 목록 API 입니다.")
interface LikeListV1ApiSpec {
    @Operation(summary = "내 좋아요 목록 조회", description = "내가 좋아요한 상품 목록을 조회합니다.")
    fun getMyLikes(
        loginId: String,
        password: String,
        pageRequest: PageRequest,
    ): ApiResponse<PageResponse<LikeV1Response.LikedProduct>>
}
