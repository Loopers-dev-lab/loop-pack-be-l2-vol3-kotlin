package com.loopers.domain.like.repository

import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.like.model.Like

interface LikeRepository {
    fun save(like: Like): Like
    fun findByUserIdAndProductIdForUpdate(userId: UserId, productId: ProductId): Like?
    fun delete(like: Like)
    fun findAllByUserId(userId: UserId): List<Like>
}
