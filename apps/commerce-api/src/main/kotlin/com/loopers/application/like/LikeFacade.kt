package com.loopers.application.like

import com.loopers.domain.like.LikeService
import com.loopers.domain.product.ProductService
import com.loopers.domain.user.event.ActionType
import com.loopers.domain.user.event.UserActionEvent
import com.loopers.event.payload.ProductLikedPayload
import com.loopers.event.payload.ProductUnlikedPayload
import com.loopers.application.outbox.OutboxPublisher
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
    private val outboxPublisher: OutboxPublisher,
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
        productService.getProduct(productId)
        val isNewLike = likeService.like(userId, productId)
        if (isNewLike) {
            // Outbox INSERT (Kafka 발행용 — 집계는 Consumer가 담당)
            outboxPublisher.publish(
                aggregateType = "CATALOG",
                aggregateId = productId.toString(),
                eventType = "LIKED",
                version = System.currentTimeMillis(),
                payload = ProductLikedPayload(userId, productId),
            )
            // 유저 행동 로깅 (인프로세스)
            eventPublisher.publishEvent(
                UserActionEvent(userId = userId, actionType = ActionType.PRODUCT_LIKED, targetId = productId),
            )
        }
    }

    @Transactional
    fun unlike(userId: Long, productId: Long) {
        productService.getProduct(productId)
        val isDeleted = likeService.unlike(userId, productId)
        if (isDeleted) {
            // Outbox INSERT (Kafka 발행용 — 집계는 Consumer가 담당)
            outboxPublisher.publish(
                aggregateType = "CATALOG",
                aggregateId = productId.toString(),
                eventType = "UNLIKED",
                version = System.currentTimeMillis(),
                payload = ProductUnlikedPayload(userId, productId),
            )
            // 유저 행동 로깅 (인프로세스)
            eventPublisher.publishEvent(
                UserActionEvent(userId = userId, actionType = ActionType.PRODUCT_UNLIKED, targetId = productId),
            )
        }
    }
}
