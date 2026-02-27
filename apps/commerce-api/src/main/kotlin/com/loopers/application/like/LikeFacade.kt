package com.loopers.application.like

import com.loopers.domain.like.LikeService
import com.loopers.domain.product.ProductService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LikeFacade(
    private val likeService: LikeService,
    private val productService: ProductService,
) {
    @Transactional
    fun likeProduct(userId: Long, productId: Long) {
        productService.findById(productId)
        val isNewLike = likeService.like(userId, productId)
        if (isNewLike) {
            productService.incrementLikesCount(productId)
        }
    }

    @Transactional
    fun unlikeProduct(userId: Long, productId: Long) {
        val wasActive = likeService.unlike(userId, productId)
        if (wasActive) {
            productService.decrementLikesCount(productId)
        }
    }
}
