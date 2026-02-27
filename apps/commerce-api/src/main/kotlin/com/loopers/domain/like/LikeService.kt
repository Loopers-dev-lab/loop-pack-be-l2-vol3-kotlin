package com.loopers.domain.like

import org.springframework.stereotype.Component

@Component
class LikeService(
    private val likeRepository: LikeRepository,
) {
    fun findLike(userId: Long, productId: Long): ProductLike? {
        return likeRepository.findByUserIdAndProductId(userId, productId)
    }

    fun createLike(userId: Long, productId: Long): ProductLike {
        return likeRepository.save(ProductLike(userId = userId, productId = productId))
    }

    fun deleteLike(productLike: ProductLike) {
        likeRepository.delete(productLike)
    }

    fun getUserLikes(userId: Long): List<ProductLike> {
        return likeRepository.findAllByUserId(userId)
    }
}
