package com.loopers.application.like

import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.domain.like.model.Like
import com.loopers.domain.like.repository.LikeRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AddLikeUseCase(
    private val likeRepository: LikeRepository,
    private val productRepository: ProductRepository,
) {
    @Transactional
    fun execute(userId: Long, productId: Long) {
        val product = productRepository.findById(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
        if (!product.isActive()) {
            throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
        }

        if (likeRepository.existsByUserIdAndProductId(userId, productId)) return

        likeRepository.save(Like(refUserId = userId, refProductId = productId))

        val lockedProduct = productRepository.findByIdForUpdate(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
        lockedProduct.increaseLikeCount()
        productRepository.save(lockedProduct)
    }
}
