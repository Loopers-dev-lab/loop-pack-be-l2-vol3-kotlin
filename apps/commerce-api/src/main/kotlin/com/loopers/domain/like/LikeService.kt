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
        return likeRepository.save(userId, productId)
    }

    fun unlike(userId: Long, productId: Long): Boolean {
        return likeRepository.deleteByUserIdAndProductId(userId, productId)
    }

    fun getLikedProductIds(userId: Long): List<Long> {
        return likeRepository.findProductIdsByUserId(userId)
    }
}
