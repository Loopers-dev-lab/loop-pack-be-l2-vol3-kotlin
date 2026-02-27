package com.loopers.application.like

data class LikeProductCriteria(
    val loginId: String,
    val productId: Long,
)

data class UnlikeProductCriteria(
    val loginId: String,
    val productId: Long,
)

data class GetLikedProductsCriteria(
    val loginId: String,
    val userId: Long,
    val page: Int,
    val size: Int,
)
