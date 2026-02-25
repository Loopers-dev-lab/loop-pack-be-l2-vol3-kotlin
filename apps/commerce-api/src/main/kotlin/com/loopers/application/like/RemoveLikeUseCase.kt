package com.loopers.application.like

import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.domain.like.repository.LikeRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RemoveLikeUseCase(
    private val likeRepository: LikeRepository,
    private val productRepository: ProductRepository,
) {
    @Transactional
    fun execute(userId: Long, productId: Long) {
        val product = productRepository.findByIdIncludeDeleted(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")

        val existing = likeRepository.findByUserIdAndProductId(userId, productId)
            ?: return

        likeRepository.delete(existing)

        if (!product.isDeleted()) {
            productRepository.decreaseLikeCount(productId)
        }
    }
}
