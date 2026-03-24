package com.loopers.domain.like

interface LikeRepository {
    fun existsByUserIdAndProductId(userId: Long, productId: Long): Boolean
    fun save(userId: Long, productId: Long): Boolean
    fun deleteByUserIdAndProductId(userId: Long, productId: Long): Boolean
    fun findProductIdsByUserId(userId: Long): List<Long>
}
