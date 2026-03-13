package com.loopers.application.user.like

import com.loopers.application.product.ProductQueryCache
import com.loopers.domain.like.ProductLikeRepository
import com.loopers.domain.product.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserProductLikeCancelUseCase(
    private val productQueryCache: ProductQueryCache,
    private val productLikeRepository: ProductLikeRepository,
    private val productRepository: ProductRepository,
) {
    @Transactional
    fun cancel(command: UserProductLikeCommand.Cancel) {
        val deleted = productLikeRepository.deleteByUserIdAndProductId(command.userId, command.productId)
        if (deleted) {
            productRepository.decrementLikeCount(command.productId)
            productQueryCache.evictDetail(command.productId)
        }
    }
}
