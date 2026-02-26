package com.loopers.application.like

import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.common.vo.UserId
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
        val existing = likeRepository.findByUserIdAndProductId(UserId(userId), ProductId(productId))
            ?: return

        likeRepository.delete(existing)

        productRepository.findByIdForUpdate(ProductId(productId))?.let { product ->
            if (product.isDeleted()) return
            product.decreaseLikeCount()
            productRepository.save(product)
        }
    }
}
