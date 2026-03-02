package com.loopers.domain.like

import com.loopers.domain.like.LikeModel
import com.loopers.domain.like.LikeRepository
import com.loopers.domain.product.ProductModel
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LikeService(
    private val likeRepository: LikeRepository,
) {

    @Transactional
    fun like(userId: Long, product: ProductModel): LikeModel {
        val existingLike = likeRepository.findByUserIdAndProductId(userId, product.id)
        if (existingLike != null) {
            return existingLike
        }

        product.increaseLikeCount()
        return likeRepository.save(LikeModel(userId = userId, productId = product.id))
    }

    @Transactional(readOnly = true)
    fun getLikedProducts(userId: Long, pageable: Pageable): List<LikeModel> {
        return likeRepository.findAllByUserId(userId, pageable)
    }

    @Transactional
    fun unlike(userId: Long, product: ProductModel) {
        val like = likeRepository.findByUserIdAndProductId(userId, product.id) ?: return
        product.decreaseLikeCount()
        likeRepository.delete(like)
    }
}
