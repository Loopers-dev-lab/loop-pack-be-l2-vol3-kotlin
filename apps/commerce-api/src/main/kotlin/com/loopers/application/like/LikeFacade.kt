package com.loopers.application.like

import com.loopers.application.product.ProductResult
import com.loopers.domain.brand.BrandService
import com.loopers.domain.event.ActionType
import com.loopers.domain.event.UserActionEvent
import com.loopers.domain.like.LikeService
import com.loopers.domain.like.event.LikeEvent
import com.loopers.domain.product.ProductInfo
import com.loopers.domain.product.ProductService
import com.loopers.domain.ratelimit.RateLimit
import com.loopers.domain.event.DomainEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LikeFacade(
    private val likeService: LikeService,
    private val productService: ProductService,
    private val brandService: BrandService,
    private val domainEventPublisher: DomainEventPublisher,
) {

    /**
     * 좋아요.
     *
     * Like 엔티티 저장만 동기로 처리하고, likeCount 업데이트는 이벤트로 분리.
     * → 집계 실패해도 좋아요 자체는 성공 (eventual consistency)
     */
    @RateLimit(key = "like:{userId}:{productId}", ttl = 1)
    @Transactional
    fun like(userId: Long, productId: Long) {
        productService.findById(productId)
        likeService.like(userId, productId)
        domainEventPublisher.publish(LikeEvent.Liked(aggregateId = productId, userId = userId))
        domainEventPublisher.publish(
            UserActionEvent(aggregateId = productId, userId = userId, actionType = ActionType.LIKE, targetId = productId),
        )
    }

    /**
     * 좋아요 취소.
     *
     * Like 엔티티 삭제만 동기로 처리하고, likeCount 감소는 이벤트로 분리.
     */
    @Transactional
    fun unlike(userId: Long, productId: Long) {
        likeService.unlike(userId, productId)
        domainEventPublisher.publish(LikeEvent.Unliked(aggregateId = productId, userId = userId))
        domainEventPublisher.publish(
            UserActionEvent(aggregateId = productId, userId = userId, actionType = ActionType.UNLIKE, targetId = productId),
        )
    }

    fun getLikedProducts(userId: Long): List<LikedProductResult> {
        val likes = likeService.findAllByUserId(userId)
        if (likes.isEmpty()) return emptyList()

        val productIds = likes.map { it.productId }
        val products = productService.findByIds(productIds)
        val productMap = products.associateBy { it.id }

        val brandIds = products.map { it.brandId }.distinct()
        val brands = brandService.findByIds(brandIds).associateBy { it.id }

        return likes.mapNotNull { likeInfo ->
            val product = productMap[likeInfo.productId] ?: return@mapNotNull null
            val brand = brands[product.brandId] ?: return@mapNotNull null
            val productResult = ProductResult.from(ProductInfo.from(product), brand.name)
            LikedProductResult.from(likeInfo, productResult)
        }
    }
}
