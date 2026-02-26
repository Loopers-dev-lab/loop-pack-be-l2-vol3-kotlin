package com.loopers.infrastructure.like

import com.loopers.domain.like.ProductLikeModel
import org.springframework.data.jpa.repository.JpaRepository

interface ProductLikeJpaRepository : JpaRepository<ProductLikeModel, Long> {
    fun findByMemberIdAndProductId(memberId: Long, productId: Long): ProductLikeModel?

    fun deleteByMemberIdAndProductId(memberId: Long, productId: Long)

    fun findAllByMemberId(memberId: Long): List<ProductLikeModel>
}
