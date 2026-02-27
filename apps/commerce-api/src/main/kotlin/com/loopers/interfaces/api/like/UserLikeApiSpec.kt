package com.loopers.interfaces.api.like

import com.loopers.application.user.AuthenticatedUserInfo
import com.loopers.interfaces.common.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "User Like API", description = "사용자 좋아요 API")
interface UserLikeApiSpec {
    @Operation(
        summary = "좋아요 목록 조회",
        description = "사용자가 좋아요한 상품 목록을 조회합니다. 본인의 좋아요 목록만 조회 가능합니다.",
    )
    fun getUserLikes(userInfo: AuthenticatedUserInfo, userId: Long): ApiResponse<List<LikeDto.UserLikeResponse>>
}
