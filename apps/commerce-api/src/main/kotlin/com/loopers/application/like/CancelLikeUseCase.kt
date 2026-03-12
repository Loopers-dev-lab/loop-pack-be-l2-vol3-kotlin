package com.loopers.application.like

import com.loopers.application.product.ProductCacheStore
import com.loopers.domain.like.LikeRepository
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.LikeErrorCode
import com.loopers.support.transaction.AfterCommit
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CancelLikeUseCase(
    private val likeRepository: LikeRepository,
    private val productRepository: ProductRepository,
    private val productCacheStore: ProductCacheStore,
) {

    @Transactional
    fun execute(userId: Long, productId: Long) {
        val like = likeRepository.findByUserIdAndProductId(userId, productId)
            ?: throw CoreException(LikeErrorCode.LIKE_NOT_FOUND)

        likeRepository.delete(like)

        productRepository.decreaseLikeCount(like.productId)

        AfterCommit.execute {
            productCacheStore.evictDetail(like.productId)
        }
    }
}
