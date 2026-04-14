package com.loopers.domain.ranking

import com.loopers.domain.metrics.EventHandledRecord
import com.loopers.domain.metrics.EventHandledRepository
import com.loopers.domain.metrics.OrderItemMetrics
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

data class ViewCount(
    val productId: Long,
    val count: Long,
)

@Component
class RankingEventService(
    private val rankingEventRepository: RankingEventRepository,
    private val eventHandledRepository: EventHandledRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun saveViewBatch(views: List<ViewCount>, eventId: String) {
        val events = views.map { view ->
            RankingEvent(
                productId = view.productId,
                eventType = RankingEventType.VIEW,
                score = RankingWeight.VIEW.weight * view.count,
                rawCount = view.count,
                eventId = eventId,
            )
        }
        rankingEventRepository.saveAll(events)
        log.info("[RankingEvent] Saved {} view events from batch", events.size)
    }

    @Transactional
    fun saveLikeEvent(productId: Long, eventId: Long) {
        if (eventHandledRepository.existsByEventId(eventId)) return

        rankingEventRepository.save(
            RankingEvent(
                productId = productId,
                eventType = RankingEventType.LIKE,
                score = RankingWeight.LIKE.weight * 1.0,
                eventId = eventId.toString(),
            ),
        )
        eventHandledRepository.save(EventHandledRecord(eventId = eventId))
        log.info("[RankingEvent] Saved like event productId={}", productId)
    }

    @Transactional
    fun saveOrderEvent(items: List<OrderItemMetrics>, eventId: Long) {
        if (eventHandledRepository.existsByEventId(eventId)) return

        val events = items.map { item ->
            RankingEvent(
                productId = item.productId,
                eventType = RankingEventType.ORDER,
                score = RankingWeight.ORDER.weight * item.productPrice * item.quantity,
                rawCount = item.quantity.toLong(),
                eventId = eventId.toString(),
            )
        }
        rankingEventRepository.saveAll(events)
        eventHandledRepository.save(EventHandledRecord(eventId = eventId))
        log.info("[RankingEvent] Saved {} order item events", events.size)
    }
}
