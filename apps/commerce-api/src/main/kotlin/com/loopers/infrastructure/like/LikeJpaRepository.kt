package com.loopers.infrastructure.like

import com.loopers.domain.like.entity.Like
import org.springframework.data.jpa.repository.JpaRepository

interface LikeJpaRepository : JpaRepository<Like, Long> {
    fun findByRefUserIdAndRefProductId(refUserId: Long, refProductId: Long): Like?
    fun findAllByRefUserIdOrderByIdDesc(refUserId: Long): List<Like>
}
