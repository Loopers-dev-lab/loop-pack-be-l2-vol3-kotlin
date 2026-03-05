package com.loopers.application.like

import com.loopers.application.product.ProductService
import com.loopers.domain.like.Like
import com.loopers.domain.like.LikeRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LikeService(
    private val likeRepository: LikeRepository,
    private val productService: ProductService,
) {

    @Transactional
    fun addLike(userId: Long, productId: Long): LikeInfo {
        productService.validateProductExistsIncludingDeleted(productId)

        val existingLike = likeRepository.findByUserIdAndProductId(userId, productId)

        if (existingLike != null) {
            if (existingLike.isDeleted()) {
                existingLike.restore()
                val saved = likeRepository.save(existingLike)
                productService.incrementLikeCount(productId)
                return LikeInfo.from(saved)
            }
            return LikeInfo.from(existingLike)
        }

        val saved = likeRepository.save(Like(userId = userId, productId = productId))
        productService.incrementLikeCount(productId)
        return LikeInfo.from(saved)
    }

    @Transactional
    fun cancelLike(userId: Long, productId: Long) {
        val like = likeRepository.findActiveByUserIdAndProductId(userId, productId) ?: return

        like.delete()
        likeRepository.save(like)
        productService.decrementLikeCount(productId)
    }

    @Transactional(readOnly = true)
    fun getUserLikes(userId: Long): List<LikeInfo> {
        return likeRepository.findAllActiveByUserId(userId).map { LikeInfo.from(it) }
    }
}
