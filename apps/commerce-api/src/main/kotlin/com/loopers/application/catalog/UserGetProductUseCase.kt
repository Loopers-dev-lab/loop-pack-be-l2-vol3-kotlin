package com.loopers.application.catalog

import com.loopers.application.UseCase
import com.loopers.config.kafka.message.ProductViewedMessage
import com.loopers.domain.catalog.BrandService
import com.loopers.domain.catalog.ProductService
import com.loopers.domain.common.event.ProductViewedEvent
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
class UserGetProductUseCase(
    private val productService: ProductService,
    private val brandService: BrandService,
    private val userService: UserService,
    private val productMetricsRedisRepository: ProductMetricsRedisRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
    private val transactionTemplate: TransactionTemplate,
) : UseCase<ViewProductCriteria, UserGetProductResult> {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val TOPIC_PRODUCT_VIEWED = "product.viewed.v1"
    }

    override fun execute(criteria: ViewProductCriteria): UserGetProductResult {
        val result = transactionTemplate.execute {
            val user = userService.getUser(criteria.loginId)
            val productInfo = productService.getProduct(criteria.productId)
            val brandInfo = brandService.findBrand(productInfo.brandId)

            eventPublisher.publishEvent(
                ProductViewedEvent(
                    userId = user.id,
                    loginId = criteria.loginId,
                    productId = criteria.productId,
                ),
            )

            Triple(user.id, productInfo, brandInfo)
        } ?: throw CoreException(ErrorType.INTERNAL_ERROR, "상품 조회 트랜잭션 실패")

        val (userId, productInfo, brandInfo) = result

        // 트랜잭션 밖 — Redis, Kafka
        productMetricsRedisRepository.incrementViewCount(criteria.productId)

        kafkaTemplate.send(TOPIC_PRODUCT_VIEWED, criteria.productId.toString(), ProductViewedMessage(
            eventId = UUID.randomUUID().toString(),
            userId = userId,
            productId = criteria.productId,
            occurredAt = ZonedDateTime.now(),
        )).whenComplete { _, ex ->
            if (ex != null) {
                log.error("상품 조회 이벤트 발행 실패 - productId: {}", criteria.productId, ex)
            }
        }

        return UserGetProductResult.from(productInfo, brandName = brandInfo?.name ?: "")
    }
}
