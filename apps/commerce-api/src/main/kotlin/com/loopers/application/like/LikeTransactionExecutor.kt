package com.loopers.application.like

import com.loopers.domain.like.LikeService
import com.loopers.domain.product.ProductService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LikeTransactionExecutor(
    private val likeService: LikeService,
    private val productService: ProductService,
) {

    @Transactional
    fun like(userId: Long, productId: Long) {
        productService.validateProductExists(productId)
        val isNewLike = likeService.like(userId, productId)
        if (isNewLike) {
            productService.incrementLikeCount(productId)
        }
    }

    @Transactional
    fun unlike(userId: Long, productId: Long) {
        productService.validateProductExists(productId)
        val isDeleted = likeService.unlike(userId, productId)
        if (isDeleted) {
            productService.decrementLikeCount(productId)
        }
    }
}
