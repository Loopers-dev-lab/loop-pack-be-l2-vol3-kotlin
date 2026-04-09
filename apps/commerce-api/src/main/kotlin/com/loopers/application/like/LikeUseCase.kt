package com.loopers.application.like

import com.loopers.application.event.KafkaIntegrationEventPublisher
import com.loopers.application.event.ProductLikeChangedEvent
import com.loopers.application.event.UserActionLogEvent
import com.loopers.application.event.UserActionType
import com.loopers.domain.brand.BrandReader
import com.loopers.domain.like.LikeReader
import com.loopers.domain.like.LikeRegister
import com.loopers.domain.like.LikeRemover
import com.loopers.domain.product.ProductReader
import com.loopers.kafka.IntegrationEvent
import com.loopers.kafka.KafkaTopics
import com.loopers.kafka.ProductLikedPayload
import com.loopers.kafka.ProductUnlikedPayload
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class LikeUseCase(
    private val likeRegister: LikeRegister,
    private val likeRemover: LikeRemover,
    private val likeReader: LikeReader,
    private val productReader: ProductReader,
    private val brandReader: BrandReader,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val kafkaIntegrationEventPublisher: KafkaIntegrationEventPublisher,
) {

    @Transactional
    fun register(memberId: Long, productId: Long): LikeInfo.Registered {
        val product = productReader.getSellingById(productId)
        val like = likeRegister.register(memberId, productId)
        val occurredAt = ZonedDateTime.now()
        applicationEventPublisher.publishEvent(
            ProductLikeChangedEvent(
                productId = productId,
                brandId = product.brandId,
                delta = 1L,
                occurredAt = occurredAt,
            ),
        )
        applicationEventPublisher.publishEvent(
            UserActionLogEvent(
                actionType = UserActionType.PRODUCT_LIKED,
                memberId = memberId,
                targetType = "product",
                targetId = productId.toString(),
                details = mapOf("likeId" to like.id),
            ),
        )
        kafkaIntegrationEventPublisher.publish(
            topic = KafkaTopics.CATALOG_EVENTS,
            event = IntegrationEvent(
                eventId = "catalog-like-added:${requireNotNull(like.id)}",
                eventType = "ProductLiked",
                aggregateType = "product",
                aggregateId = productId.toString(),
                key = productId.toString(),
                version = 1L,
                occurredAt = occurredAt,
                payload = ProductLikedPayload(
                    likeId = requireNotNull(like.id),
                    productId = productId,
                    memberId = memberId,
                ),
            ),
        )
        return LikeInfo.Registered.from(like)
    }

    @Transactional
    fun remove(likeId: Long, memberId: Long) {
        val like = likeRemover.remove(likeId, memberId)
        val product = productReader.getById(like.productId)
        val occurredAt = ZonedDateTime.now()
        applicationEventPublisher.publishEvent(
            ProductLikeChangedEvent(
                productId = like.productId,
                brandId = product.brandId,
                delta = -1L,
                occurredAt = occurredAt,
            ),
        )
        applicationEventPublisher.publishEvent(
            UserActionLogEvent(
                actionType = UserActionType.PRODUCT_UNLIKED,
                memberId = memberId,
                targetType = "product",
                targetId = like.productId.toString(),
                details = mapOf("likeId" to like.id),
            ),
        )
        kafkaIntegrationEventPublisher.publish(
            topic = KafkaTopics.CATALOG_EVENTS,
            event = IntegrationEvent(
                eventId = "catalog-like-removed:${requireNotNull(like.id)}",
                eventType = "ProductUnliked",
                aggregateType = "product",
                aggregateId = like.productId.toString(),
                key = like.productId.toString(),
                version = 1L,
                occurredAt = occurredAt,
                payload = ProductUnlikedPayload(
                    likeId = requireNotNull(like.id),
                    productId = like.productId,
                    memberId = memberId,
                ),
            ),
        )
    }

    @Transactional(readOnly = true)
    fun getMyLikes(memberId: Long): List<LikeInfo.Detail> {
        val likes = likeReader.getAllByMemberId(memberId)
        if (likes.isEmpty()) return emptyList()

        val productIds = likes.map { it.productId }
        val productMap = productReader.getAllByIds(productIds).associateBy { it.id }

        val brandIds = productMap.values.map { it.brandId }.distinct()
        val brandMap = brandReader.getAllByIds(brandIds).associateBy { it.id }

        return likes.mapNotNull { like ->
            val product = productMap[like.productId] ?: return@mapNotNull null
            val brand = brandMap[product.brandId]
            LikeInfo.Detail.from(like, product, brand)
        }
    }
}
