package com.loopers.application.like

import com.loopers.application.event.LikeToggledEvent
import com.loopers.application.outbox.OutboxService
import com.loopers.application.product.ProductService
import com.loopers.domain.like.Like
import com.loopers.domain.like.LikeRepository
import com.loopers.event.KafkaTopics
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LikeService(
    private val likeRepository: LikeRepository,
    private val productService: ProductService,
    private val eventPublisher: ApplicationEventPublisher,
    private val outboxService: OutboxService,
) {

    @Transactional
    fun addLike(userId: Long, productId: Long): LikeInfo {
        productService.validateProductExistsIncludingDeleted(productId)

        val existingLike = likeRepository.findByUserIdAndProductId(userId, productId)

        if (existingLike != null) {
            if (existingLike.isDeleted()) {
                existingLike.restore()
                val saved = likeRepository.save(existingLike)
                val event = LikeToggledEvent(userId = userId, productId = productId, liked = true)
                saveOutbox(event)
                eventPublisher.publishEvent(event)
                return LikeInfo.from(saved)
            }
            return LikeInfo.from(existingLike)
        }

        val saved = likeRepository.save(Like(userId = userId, productId = productId))
        val event = LikeToggledEvent(userId = userId, productId = productId, liked = true)
        saveOutbox(event)
        eventPublisher.publishEvent(event)
        return LikeInfo.from(saved)
    }

    @Transactional
    fun cancelLike(userId: Long, productId: Long) {
        val like = likeRepository.findActiveByUserIdAndProductId(userId, productId) ?: return

        like.delete()
        likeRepository.save(like)
        val event = LikeToggledEvent(userId = userId, productId = productId, liked = false)
        saveOutbox(event)
        eventPublisher.publishEvent(event)
    }

    @Transactional(readOnly = true)
    fun getUserLikes(userId: Long): List<LikeInfo> {
        return likeRepository.findAllActiveByUserId(userId).map { LikeInfo.from(it) }
    }

    private fun saveOutbox(event: LikeToggledEvent) {
        val eventType = if (event.liked) "PRODUCT_LIKED" else "PRODUCT_UNLIKED"
        outboxService.save(
            aggregateType = "PRODUCT",
            aggregateId = event.productId.toString(),
            eventType = eventType,
            topic = KafkaTopics.CATALOG_EVENTS,
            partitionKey = event.productId.toString(),
            payload = event,
        )
    }
}
