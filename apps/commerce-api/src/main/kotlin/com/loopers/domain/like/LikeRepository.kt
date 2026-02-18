package com.loopers.domain.like

interface LikeRepository {
    fun addLike(userId: Long, productId: Long): Boolean
    fun removeLike(userId: Long, productId: Long): Int
    fun findAllByUserId(userId: Long): List<Like>
}
