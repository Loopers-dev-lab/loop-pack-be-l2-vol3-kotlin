package com.loopers.interfaces.api.like

import com.loopers.domain.user.User
import com.loopers.interfaces.common.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Like API", description = "좋아요 API")
interface LikeApiSpec {
    @Operation(
        summary = "좋아요 등록",
        description = "상품에 좋아요를 등록합니다. 이미 좋아요한 경우 멱등하게 처리됩니다.",
    )
    fun like(user: User, productId: Long): ApiResponse<Unit>

    @Operation(
        summary = "좋아요 취소",
        description = "상품의 좋아요를 취소합니다. 좋아요하지 않은 경우 멱등하게 처리됩니다.",
    )
    fun unlike(user: User, productId: Long): ApiResponse<Unit>
}
