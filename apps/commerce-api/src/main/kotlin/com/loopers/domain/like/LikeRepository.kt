package com.loopers.domain.like

interface LikeRepository {
    fun save(like: Like): Like
    fun findByUserIdAndProductId(userId: Long, productId: Long): Like?
    fun findActiveByUserIdAndProductId(userId: Long, productId: Long): Like?
    fun findAllActiveByUserId(userId: Long): List<Like>
}
