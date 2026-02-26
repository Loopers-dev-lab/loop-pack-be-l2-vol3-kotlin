package com.loopers.domain.like

interface LikeRepository {
    fun findByUserIdAndProductId(userId: Long, productId: Long): Like?
    fun existsByUserIdAndProductId(userId: Long, productId: Long): Boolean
    fun findAllByUserId(userId: Long): List<Like>
    fun save(like: Like): Like
    fun delete(like: Like)
}
