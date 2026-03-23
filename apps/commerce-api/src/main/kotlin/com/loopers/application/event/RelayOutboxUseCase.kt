package com.loopers.application.event

import com.loopers.domain.outbox.repository.CatalogOutboxRepository
import com.loopers.domain.outbox.repository.CouponOutboxRepository
import com.loopers.domain.outbox.repository.OrderOutboxRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

@Component
class RelayOutboxUseCase(
    private val catalogOutboxRepository: CatalogOutboxRepository,
    private val orderOutboxRepository: OrderOutboxRepository,
    private val couponOutboxRepository: CouponOutboxRepository,
    private val outboxEventPublisher: OutboxEventPublisher,
    private val transactionTemplate: TransactionTemplate,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun execute() {
        relayCatalogEvents()
        relayOrderEvents()
        relayCouponEvents()
    }

    private fun relayCatalogEvents() {
        val unpublished = catalogOutboxRepository.findAllUnpublished()
        for (outbox in unpublished) {
            try {
                outboxEventPublisher.publish(
                    topic = TOPIC_CATALOG_EVENTS,
                    key = outbox.productId.toString(),
                    payload = mapOf(
                        "eventId" to outbox.eventId,
                        "eventType" to outbox.eventType,
                        "productId" to outbox.productId,
                        "userId" to outbox.userId,
                    ),
                )
                transactionTemplate.execute {
                    outbox.markPublished()
                    catalogOutboxRepository.save(outbox)
                }
            } catch (ex: Exception) {
                log.error("CatalogOutbox 발행 실패: id={}", outbox.id, ex)
            }
        }
    }

    private fun relayOrderEvents() {
        val unpublished = orderOutboxRepository.findAllUnpublished()
        for (outbox in unpublished) {
            try {
                outboxEventPublisher.publish(
                    topic = TOPIC_ORDER_EVENTS,
                    key = outbox.orderId.toString(),
                    payload = buildMap {
                        put("eventId", outbox.eventId)
                        put("eventType", outbox.eventType)
                        put("orderId", outbox.orderId)
                        put("userId", outbox.userId)
                        put("totalAmount", outbox.totalAmount)
                        outbox.reason?.let { put("reason", it) }
                        outbox.productId?.let { put("productId", it) }
                        outbox.quantity?.let { put("quantity", it) }
                    },
                )
                transactionTemplate.execute {
                    outbox.markPublished()
                    orderOutboxRepository.save(outbox)
                }
            } catch (ex: Exception) {
                log.error("OrderOutbox 발행 실패: id={}", outbox.id, ex)
            }
        }
    }

    // Step 3 (선착순 쿠폰 Kafka 발급)에서 CouponOutbox 저장 진입점이 추가될 예정
    private fun relayCouponEvents() {
        val unpublished = couponOutboxRepository.findAllUnpublished()
        for (outbox in unpublished) {
            try {
                outboxEventPublisher.publish(
                    topic = TOPIC_COUPON_ISSUE_REQUESTS,
                    key = outbox.couponId.toString(),
                    payload = mapOf(
                        "eventId" to outbox.eventId,
                        "eventType" to outbox.eventType,
                        "couponId" to outbox.couponId,
                        "userId" to outbox.userId,
                    ),
                )
                transactionTemplate.execute {
                    outbox.markPublished()
                    couponOutboxRepository.save(outbox)
                }
            } catch (ex: Exception) {
                log.error("CouponOutbox 발행 실패: id={}", outbox.id, ex)
            }
        }
    }

    companion object {
        const val TOPIC_CATALOG_EVENTS = "catalog-events"
        const val TOPIC_ORDER_EVENTS = "order-events"
        const val TOPIC_COUPON_ISSUE_REQUESTS = "coupon-issue-requests"
    }
}
