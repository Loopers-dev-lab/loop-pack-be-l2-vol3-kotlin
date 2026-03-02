package com.loopers.domain.like

import com.loopers.support.PageResult

interface LikeRepository {
    fun findByUserIdAndProductId(userId: Long, productId: Long): Like?
    fun existsByUserIdAndProductId(userId: Long, productId: Long): Boolean
    fun findActiveLikesByUserId(userId: Long, page: Int, size: Int): PageResult<Like>
    fun save(like: Like): Like
    fun delete(like: Like)
}
