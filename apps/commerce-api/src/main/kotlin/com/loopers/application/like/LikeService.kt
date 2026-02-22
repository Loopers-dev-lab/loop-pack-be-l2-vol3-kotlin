package com.loopers.application.like

import com.loopers.domain.like.LikeModel
import com.loopers.domain.like.LikeRepository
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LikeService(
    private val likeRepository: LikeRepository,
    private val productRepository: ProductRepository,
) {

    @Transactional
    fun like(userId: Long, productId: Long): LikeModel {
        val product = productRepository.findById(productId) ?: throw CoreException(
            errorType = ErrorType.NOT_FOUND,
            customMessage = "해당 상품을 찾을 수 없습니다.",
        )

        val existingLike = likeRepository.findByUserIdAndProductId(userId, productId)
        if (existingLike != null) {
            return existingLike
        }

        val like = LikeModel(userId = userId, productId = productId)
        product.increaseLikeCount()

        return likeRepository.save(like)
    }

    @Transactional
    fun unlike(userId: Long, productId: Long) {
        val product = productRepository.findById(productId) ?: throw CoreException(
            errorType = ErrorType.NOT_FOUND,
            customMessage = "해당 상품을 찾을 수 없습니다.",
        )

        val like = likeRepository.findByUserIdAndProductId(userId, productId) ?: return

        product.decreaseLikeCount()
        likeRepository.delete(like)
    }
}
