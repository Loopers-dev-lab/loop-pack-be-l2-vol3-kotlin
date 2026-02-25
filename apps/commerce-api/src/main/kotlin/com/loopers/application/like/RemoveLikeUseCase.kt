package com.loopers.application.like

import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.domain.like.repository.LikeRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RemoveLikeUseCase(
    private val likeRepository: LikeRepository,
    private val productRepository: ProductRepository,
) {
    @Transactional
    fun execute(userId: Long, productId: Long) {
        val existing = likeRepository.findByUserIdAndProductId(userId, productId)
            ?: return

        likeRepository.delete(existing)

        productRepository.findByIdForUpdate(productId)?.let { product ->
            product.decreaseLikeCount()
            productRepository.save(product)
        }
    }
}
