package com.loopers.domain.like

interface LikeRepository {
    fun save(like: Like): Like
    fun findByUserIdAndProductId(userId: Long, productId: Long): Like?
    fun findAllByUserId(userId: Long): List<Like>
    fun delete(like: Like)
    fun deleteAllByProductId(productId: Long)
}
