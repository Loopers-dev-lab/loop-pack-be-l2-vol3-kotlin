package com.loopers.application.like

import com.loopers.application.event.LikeActionType
import com.loopers.application.event.LikeChangedEvent
import com.loopers.domain.like.LikeService
import com.loopers.domain.product.ProductService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LikeFacade(
    private val likeService: LikeService,
    private val productService: ProductService,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun likeProduct(userId: Long, productId: Long) {
        productService.findById(productId)
        val isNewLike = likeService.like(userId, productId)
        if (isNewLike) {
            applicationEventPublisher.publishEvent(
                LikeChangedEvent(
                    userId = userId,
                    productId = productId,
                    actionType = LikeActionType.LIKE,
                ),
            )
        }
    }

    @Transactional
    fun unlikeProduct(userId: Long, productId: Long) {
        val wasActive = likeService.unlike(userId, productId)
        if (wasActive) {
            applicationEventPublisher.publishEvent(
                LikeChangedEvent(
                    userId = userId,
                    productId = productId,
                    actionType = LikeActionType.UNLIKE,
                ),
            )
        }
    }
}
