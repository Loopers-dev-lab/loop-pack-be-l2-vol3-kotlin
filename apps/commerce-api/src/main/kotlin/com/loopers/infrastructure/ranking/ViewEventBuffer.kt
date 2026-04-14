package com.loopers.infrastructure.ranking

import com.loopers.domain.event.EventTopics
import com.loopers.domain.event.OutboxEventService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.LongAdder

@Component
class ViewEventBuffer(
    private val outboxEventService: OutboxEventService,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val buffer = ConcurrentHashMap<Long, LongAdder>()

    fun record(productId: Long) {
        buffer.computeIfAbsent(productId) { LongAdder() }.increment()
    }

    @Scheduled(fixedDelay = 5000)
    fun flush() {
        if (buffer.isEmpty()) return

        val snapshot = mutableListOf<ViewCount>()
        val iter = buffer.entries.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            val count = entry.value.sumThenReset()
            if (count > 0) {
                snapshot.add(ViewCount(productId = entry.key, count = count))
            }
            if (entry.value.sum() == 0L) {
                iter.remove()
            }
        }

        if (snapshot.isEmpty()) return

        outboxEventService.saveOutboxEvent(
            aggregateType = "Ranking",
            aggregateId = "view-batch",
            eventType = "ProductViewedBatch",
            topic = EventTopics.CATALOG_EVENTS,
            event = ViewBatchPayload(views = snapshot),
        )

        log.info("[ViewEventBuffer] Flushed {} product view counts", snapshot.size)
    }

    data class ViewCount(
        val productId: Long,
        val count: Long,
    )

    data class ViewBatchPayload(
        val views: List<ViewCount>,
    )
}
