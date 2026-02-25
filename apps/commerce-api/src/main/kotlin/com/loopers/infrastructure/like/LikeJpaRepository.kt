package com.loopers.infrastructure.like

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface LikeJpaRepository : JpaRepository<LikeEntity, Long> {
    fun findAllByMemberId(memberId: Long): List<LikeEntity>
    fun existsByMemberIdAndProductId(memberId: Long, productId: Long): Boolean
    fun countByProductId(productId: Long): Long

    @Query("SELECT l.productId, COUNT(l) FROM LikeEntity l WHERE l.productId IN :productIds GROUP BY l.productId")
    fun countByProductIdIn(productIds: List<Long>): List<Array<Any>>
}
