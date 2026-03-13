package com.loopers.application.like

import com.loopers.domain.like.LikeService
import com.loopers.domain.product.ProductService
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Component

@Component
class LikeFacade(
    private val likeService: LikeService,
    private val productService: ProductService,
) {
    companion object {
        private const val MAX_RETRY = 3
    }

    fun likeProduct(userId: Long, productId: Long) {
        productService.findById(productId)
        val isNewLike = likeService.like(userId, productId)
        if (isNewLike) {
            retryOnOptimisticLock { productService.incrementLikesCount(productId) }
        }
    }

    fun unlikeProduct(userId: Long, productId: Long) {
        val wasActive = likeService.unlike(userId, productId)
        if (wasActive) {
            retryOnOptimisticLock { productService.decrementLikesCount(productId) }
        }
    }

    private fun retryOnOptimisticLock(action: () -> Unit) {
        var attempts = 0
        while (true) {
            try {
                action()
                return
            } catch (e: ObjectOptimisticLockingFailureException) {
                attempts++
                if (attempts >= MAX_RETRY) {
                    throw e
                }
            }
        }
    }
}
