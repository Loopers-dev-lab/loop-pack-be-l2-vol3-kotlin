package com.loopers.domain.like

interface LikeRepository {
    fun addLike(userId: Long, productId: Long): Boolean
    fun removeLike(userId: Long, productId: Long): Int
    fun findAllByUserId(userId: Long, page: Int, size: Int): List<Like>
    fun countByUserId(userId: Long): Long
}
