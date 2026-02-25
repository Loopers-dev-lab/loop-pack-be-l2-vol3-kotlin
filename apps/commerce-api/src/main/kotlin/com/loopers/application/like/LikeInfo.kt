package com.loopers.application.like

import com.loopers.domain.like.Like

class LikeInfo {

    data class Registered(
        val id: Long,
        val memberId: Long,
        val productId: Long,
    ) {
        companion object {
            fun from(like: Like) = Registered(
                id = requireNotNull(like.id) { "좋아요 저장 후 ID가 할당되지 않았습니다." },
                memberId = like.memberId,
                productId = like.productId,
            )
        }
    }

    data class Detail(
        val id: Long,
        val productId: Long,
        val productName: String,
        val price: Long,
        val brandName: String,
    )
}
