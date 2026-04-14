package com.loopers.domain.catalog

import com.loopers.config.kafka.message.ProductLikedMessage
import com.loopers.config.kafka.message.ProductViewedMessage
import com.loopers.domain.event.EventHandledModel
import com.loopers.domain.event.EventHandledRepository
import com.loopers.infrastructure.catalog.ProductRankRedisRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
class ProductEventService(
    private val eventHandledRepository: EventHandledRepository,
    private val productRankRedisRepository: ProductRankRedisRepository,
    private val transactionTemplate: TransactionTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun handleProductViewed(message: ProductViewedMessage) {
        val processed = saveEventIfNew(message.eventId, "PRODUCT_VIEWED") {
            log.info("상품 조회 이벤트 수신 - productId: {}, userId: {}", message.productId, message.userId)
        }

        // DB 커밋 이후에 Redis 반영 — 롤백 시 이중 가산 방지
        if (processed) {
            productRankRedisRepository.incrementView(message.productId, message.occurredAt.toLocalDate())
        }
    }

    fun handleProductLiked(message: ProductLikedMessage) {
        val processed = saveEventIfNew(message.eventId, "PRODUCT_LIKED") {
            log.info("상품 좋아요 이벤트 수신 - productId: {}, userId: {}", message.productId, message.userId)
        }

        if (processed) {
            productRankRedisRepository.incrementLike(message.productId, message.occurredAt.toLocalDate())
        }
    }

    /**
     * eventId 기준 멱등 처리 — DB 트랜잭션 안에서 중복 확인 + 저장만 수행한다.
     * Redis 등 외부 쓰기는 이 메서드 밖(커밋 이후)에서 호출해야 롤백 시 이중 반영을 막을 수 있다.
     *
     * @return true면 신규 이벤트로 처리됨, false면 이미 처리된 이벤트
     */
    private fun saveEventIfNew(eventId: String, eventType: String, onNew: () -> Unit): Boolean {
        return transactionTemplate.execute {
            if (eventHandledRepository.existsByEventId(eventId)) {
                log.info("이미 처리된 이벤트 - eventId: {}", eventId)
                return@execute false
            }

            onNew()

            eventHandledRepository.save(
                EventHandledModel(
                    eventId = eventId,
                    eventType = eventType,
                ),
            )
            true
        } ?: false
    }
}
