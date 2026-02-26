package com.loopers.application.like

import com.loopers.domain.like.ProductLikeModel
import com.loopers.domain.like.ProductLikeRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LikeService(
    private val productLikeRepository: ProductLikeRepository,
) {
    @Transactional
    fun like(memberId: Long, productId: Long) {
        val existing = productLikeRepository.findByMemberIdAndProductId(memberId, productId)
        if (existing != null) return // 이미 좋아요한 경우 멱등 처리 (BR-L2)
        productLikeRepository.save(ProductLikeModel(memberId = memberId, productId = productId))
    }

    @Transactional
    fun unlike(memberId: Long, productId: Long) {
        productLikeRepository.deleteByMemberIdAndProductId(memberId, productId)
    }

    @Transactional(readOnly = true)
    fun getLikedProductIds(memberId: Long): List<Long> {
        return productLikeRepository.findAllByMemberId(memberId)
            .map { it.productId }
    }
}
