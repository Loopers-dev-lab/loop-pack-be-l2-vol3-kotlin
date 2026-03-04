package com.loopers.domain.like

interface LikeRepository {
    fun save(like: Like): Like
    fun delete(like: Like)
    fun findById(id: Long): Like?
    fun findAllByMemberId(memberId: Long): List<Like>
    fun existsByMemberIdAndProductId(memberId: Long, productId: Long): Boolean
    fun countByProductId(productId: Long): Long
    fun countByProductIds(productIds: List<Long>): Map<Long, Long>
}
