package com.loopers.application.like

import com.loopers.domain.like.LikeRepository
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.LikeErrorCode
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CancelLikeUseCase(
    private val likeRepository: LikeRepository,
    private val productRepository: ProductRepository,
) {

    @Transactional
    fun execute(userId: Long, productId: Long) {
        val like = likeRepository.findByUserIdAndProductId(userId, productId)
            ?: throw CoreException(LikeErrorCode.LIKE_NOT_FOUND)

        likeRepository.delete(like)

        val product = productRepository.findByIdOrNull(like.productId)
        product?.decreaseLikeCount()
    }
}
