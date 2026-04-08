package com.loopers.application.like

import com.loopers.application.UseCase
import com.loopers.config.kafka.message.ProductLikedMessage
import com.loopers.domain.catalog.ProductService
import com.loopers.domain.common.event.ProductLikedEvent
import com.loopers.domain.like.LikeProductCommand
import com.loopers.domain.like.ProductLikeService
import com.loopers.domain.user.UserService
import com.loopers.infrastructure.catalog.ProductMetricsRedisRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.time.ZonedDateTime
import java.util.UUID

@Component
class UserLikeProductUseCase(
    private val userService: UserService,
    private val productService: ProductService,
    private val productLikeService: ProductLikeService,
    private val productMetricsRedisRepository: ProductMetricsRedisRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
    private val transactionTemplate: TransactionTemplate,
) : UseCase<LikeProductCriteria, Unit> {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val TOPIC_PRODUCT_LIKED = "product.liked.v1"
    }

    override fun execute(criteria: LikeProductCriteria) {
        val (userId, created) = transactionTemplate.execute {
            val user = userService.getUser(criteria.loginId)
            productService.getProduct(criteria.productId)
            val created = productLikeService.like(LikeProductCommand(userId = user.id, productId = criteria.productId))

            eventPublisher.publishEvent(
                ProductLikedEvent(
                    userId = user.id,
                    loginId = criteria.loginId,
                    productId = criteria.productId,
                    isNewLike = created,
                ),
            )

            Pair(user.id, created)
        } ?: throw CoreException(ErrorType.INTERNAL_ERROR, "좋아요 트랜잭션 실패")

        // 트랜잭션 밖 — Redis, Kafka
        if (created) {
            productMetricsRedisRepository.incrementLikeCount(criteria.productId)

            kafkaTemplate.send(TOPIC_PRODUCT_LIKED, criteria.productId.toString(), ProductLikedMessage(
                eventId = UUID.randomUUID().toString(),
                userId = userId,
                productId = criteria.productId,
                occurredAt = ZonedDateTime.now(),
            )).whenComplete { _, ex ->
                if (ex != null) {
                    log.error("상품 좋아요 이벤트 발행 실패 - productId: {}", criteria.productId, ex)
                }
            }
        }
    }
}
