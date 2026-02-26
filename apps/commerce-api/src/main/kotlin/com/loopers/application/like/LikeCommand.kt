package com.loopers.application.like

class LikeCommand {

    data class Create(
        val userId: Long,
        val productId: Long,
    )
}
