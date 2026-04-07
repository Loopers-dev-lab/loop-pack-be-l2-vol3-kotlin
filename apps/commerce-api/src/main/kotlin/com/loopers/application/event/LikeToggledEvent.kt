package com.loopers.application.event

data class LikeToggledEvent(
    val userId: Long,
    val productId: Long,
    val liked: Boolean,
)
