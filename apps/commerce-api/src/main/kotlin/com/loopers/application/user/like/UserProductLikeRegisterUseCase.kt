package com.loopers.application.user.like

import com.loopers.application.product.ProductQueryCache
import com.loopers.domain.like.ProductLike
import com.loopers.domain.like.ProductLikeRepository
import com.loopers.domain.product.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserProductLikeRegisterUseCase(
    private val productQueryCache: ProductQueryCache,
    private val productRepository: ProductRepository,
    private val productLikeRepository: ProductLikeRepository,
) {
    @Transactional
    fun register(command: UserProductLikeCommand.Register) {
        val product = productRepository.findById(command.productId) ?: return
        if (!product.isActive()) return

        val like = ProductLike.register(command.userId, command.productId)
        val created = productLikeRepository.save(like)
        if (created) {
            productRepository.incrementLikeCount(command.productId)
            productQueryCache.evictDetail(command.productId)
        }
    }
}
