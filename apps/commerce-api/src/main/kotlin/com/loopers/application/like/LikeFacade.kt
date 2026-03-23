package com.loopers.application.like

import com.loopers.domain.like.LikeService
import com.loopers.domain.like.event.ProductLikedEvent
import com.loopers.domain.like.event.ProductUnlikedEvent
import com.loopers.domain.product.ProductService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LikeFacade(
    private val likeService: LikeService,
    private val productService: ProductService,
    private val eventPublisher: ApplicationEventPublisher,
) {

    @Transactional(readOnly = true)
    fun getUserLikes(authenticatedUserId: Long, userId: Long): List<LikeInfo> {
        if (authenticatedUserId != userId) {
            throw CoreException(ErrorType.FORBIDDEN, "타 유저의 정보에 접근할 수 없습니다")
        }
        val productIds = likeService.getLikedProductIds(userId)
        if (productIds.isEmpty()) return emptyList()
        val products = productService.getProductsByIds(productIds)
        return products.map { LikeInfo.from(it) }
    }

    @Transactional
    fun like(userId: Long, productId: Long) {
        // 비관적 락: 같은 상품에 대한 동시 like INSERT의 TOCTOU 방지
        productService.getProductWithLock(productId)
        val isNewLike = likeService.like(userId, productId)
        if (isNewLike) {
            eventPublisher.publishEvent(ProductLikedEvent(userId, productId))
        }
    }

    @Transactional
    fun unlike(userId: Long, productId: Long) {
        productService.getProductWithLock(productId)
        val isDeleted = likeService.unlike(userId, productId)
        if (isDeleted) {
            eventPublisher.publishEvent(ProductUnlikedEvent(userId, productId))
        }
    }
}
