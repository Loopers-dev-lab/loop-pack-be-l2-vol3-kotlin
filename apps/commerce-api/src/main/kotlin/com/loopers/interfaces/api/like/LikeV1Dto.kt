package com.loopers.interfaces.api.like

import com.loopers.domain.like.Like
import io.swagger.v3.oas.annotations.media.Schema
import java.time.ZonedDateTime

class LikeV1Dto {

    @Schema(description = "좋아요 응답")
    data class LikeResponse(
        @Schema(description = "좋아요 ID", example = "1")
        val id: Long,
        @Schema(description = "상품 ID", example = "1")
        val productId: Long,
        @Schema(description = "생성일시")
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(like: Like): LikeResponse {
                return LikeResponse(
                    id = like.id,
                    productId = like.productId,
                    createdAt = like.createdAt,
                )
            }
        }
    }
}
