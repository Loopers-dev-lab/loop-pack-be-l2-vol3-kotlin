package com.loopers.domain.like

import org.springframework.stereotype.Component

@Component
class LikeService(
    private val likeRepository: LikeRepository,
) {

    fun like(userId: Long, productId: Long): Boolean {
        if (likeRepository.existsByUserIdAndProductId(userId, productId)) {
            return false
        }
        likeRepository.save(Like(userId = userId, productId = productId))
        return true
    }

    fun unlike(userId: Long, productId: Long): Boolean {
        return likeRepository.deleteByUserIdAndProductId(userId, productId)
    }

    fun getLikedProductIds(userId: Long): List<Long> {
        return likeRepository.findProductIdsByUserId(userId)
    }
}
