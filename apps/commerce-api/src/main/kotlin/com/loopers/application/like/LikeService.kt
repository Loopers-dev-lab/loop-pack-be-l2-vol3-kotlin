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
                return LikeInfo.from(likeRepository.save(existingLike))
            }
            return LikeInfo.from(existingLike)
        }

        return LikeInfo.from(likeRepository.save(Like(userId = userId, productId = productId)))
    }

    @Transactional
    fun cancelLike(userId: Long, productId: Long) {
        val like = likeRepository.findActiveByUserIdAndProductId(userId, productId) ?: return

        like.delete()
        likeRepository.save(like)
    }

    @Transactional(readOnly = true)
    fun getUserLikes(userId: Long): List<LikeInfo> {
        return likeRepository.findAllActiveByUserId(userId).map { LikeInfo.from(it) }
    }
}
