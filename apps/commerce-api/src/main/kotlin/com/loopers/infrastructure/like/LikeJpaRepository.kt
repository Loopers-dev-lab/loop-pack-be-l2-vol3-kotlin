package com.loopers.infrastructure.like

import com.loopers.domain.like.LikeModel
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface LikeJpaRepository : JpaRepository<LikeModel, Long> {
    fun findByUserIdAndProductId(userId: Long, productId: Long): LikeModel?
    fun existsByUserIdAndProductId(userId: Long, productId: Long): Boolean
    fun findAllByUserId(userId: Long, pageable: Pageable): List<LikeModel>
}
