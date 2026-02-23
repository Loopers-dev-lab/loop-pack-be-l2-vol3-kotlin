package com.loopers.domain.like

interface LikeRepository {
    fun save(like: Like): Like
    fun existsByUserIdAndProductId(userId: Long, productId: Long): Boolean
    fun deleteByUserIdAndProductId(userId: Long, productId: Long): Boolean
}
