package com.loopers.domain.like

import org.springframework.data.domain.Pageable

interface LikeRepository {
    fun save(like: LikeModel): LikeModel
    fun findByUserIdAndProductId(userId: Long, productId: Long): LikeModel?
    fun existsByUserIdAndProductId(userId: Long, productId: Long): Boolean
    fun delete(like: LikeModel)
    fun findAllByUserId(userId: Long, pageable: Pageable): List<LikeModel>
}
