package com.loopers.infrastructure.like

import org.springframework.data.jpa.repository.JpaRepository

interface ProductLikeJpaRepository : JpaRepository<ProductLikeJpaModel, Long> {
    fun findByMemberIdAndProductId(memberId: Long, productId: Long): ProductLikeJpaModel?

    fun deleteByMemberIdAndProductId(memberId: Long, productId: Long)

    fun findAllByMemberId(memberId: Long): List<ProductLikeJpaModel>
}
