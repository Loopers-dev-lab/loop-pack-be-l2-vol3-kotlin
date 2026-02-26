package com.loopers.infrastructure.like

import com.loopers.domain.like.Like
import org.springframework.data.jpa.repository.JpaRepository

interface LikeJpaRepository : JpaRepository<Like, Long> {

    fun findByUserIdAndProductId(userId: Long, productId: Long): Like?

    fun findByUserIdAndProductIdAndDeletedAtIsNull(userId: Long, productId: Long): Like?

    fun findAllByUserIdAndDeletedAtIsNull(userId: Long): List<Like>
}
