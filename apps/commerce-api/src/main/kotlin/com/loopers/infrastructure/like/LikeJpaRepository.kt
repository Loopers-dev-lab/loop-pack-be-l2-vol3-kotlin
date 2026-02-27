package com.loopers.infrastructure.like

import com.loopers.domain.like.LikeModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface LikeJpaRepository : JpaRepository<LikeModel, Long> {
    fun findByUserIdAndProductId(userId: Long, productId: Long): LikeModel?
    fun findAllByUserIdAndDeletedAtIsNull(userId: Long, pageable: Pageable): Page<LikeModel>
}
