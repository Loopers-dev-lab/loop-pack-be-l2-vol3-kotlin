package com.loopers.domain.like

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface LikeRepository {
    fun save(like: LikeModel): LikeModel
    fun findByUserIdAndProductId(userId: Long, productId: Long): LikeModel?
    fun findAllByUserIdAndDeletedAtIsNull(userId: Long, pageable: Pageable): Page<LikeModel>
}
