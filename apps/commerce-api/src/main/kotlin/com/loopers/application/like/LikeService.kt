package com.loopers.application.like

import com.loopers.domain.like.ProductLikeModel
import com.loopers.domain.like.ProductLikeRepository
import org.springframework.stereotype.Component

@Component
class LikeService(
    private val productLikeRepository: ProductLikeRepository,
) {
    fun like(memberId: Long, productId: Long) {
        productLikeRepository.save(ProductLikeModel(memberId = memberId, productId = productId))
    }

    fun exists(memberId: Long, productId: Long): Boolean {
        return productLikeRepository.findByMemberIdAndProductId(memberId, productId) != null
    }

    fun unlike(memberId: Long, productId: Long) {
        productLikeRepository.deleteByMemberIdAndProductId(memberId, productId)
    }

    fun getLikedProductIds(memberId: Long): List<Long> {
        return productLikeRepository.findAllByMemberId(memberId)
            .map { it.productId }
    }
}
