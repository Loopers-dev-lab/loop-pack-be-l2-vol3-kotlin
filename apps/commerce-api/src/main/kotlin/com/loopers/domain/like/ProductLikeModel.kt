package com.loopers.domain.like

import java.time.ZonedDateTime

data class ProductLikeModel(
    val id: Long = 0,
    val memberId: Long,
    val productId: Long,
    val createdAt: ZonedDateTime? = null,
    val updatedAt: ZonedDateTime? = null,
    val deletedAt: ZonedDateTime? = null,
)
