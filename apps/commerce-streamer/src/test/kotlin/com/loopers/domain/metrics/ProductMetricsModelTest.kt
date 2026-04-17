package com.loopers.domain.metrics

import com.loopers.config.kafka.event.CatalogEventMessage
import com.loopers.config.kafka.event.CatalogEventType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

@DisplayName("ProductMetricsModel")
class ProductMetricsModelTest {

    @DisplayName("ORDER_COMPLETED 이벤트를 적용하면 salesCount가 delta만큼 증가한다")
    @Test
    fun appliesOrderCompletedEvent() {
        // arrange
        val model = ProductMetricsModel(productId = 1L)
        val event = catalogEvent(
            eventType = CatalogEventType.ORDER_COMPLETED,
            delta = 3,
            version = 1,
        )

        // act
        model.apply(event)

        // assert
        assertThat(model.salesCount).isEqualTo(3)
        assertThat(model.likesCount).isEqualTo(0)
        assertThat(model.viewsCount).isEqualTo(0)
    }

    @DisplayName("ORDER_COMPLETED 이벤트를 여러 번 적용하면 salesCount가 누적된다")
    @Test
    fun accumulatesSalesCount() {
        // arrange
        val model = ProductMetricsModel(productId = 1L)

        // act
        model.apply(catalogEvent(eventType = CatalogEventType.ORDER_COMPLETED, delta = 2, version = 1))
        model.apply(catalogEvent(eventType = CatalogEventType.ORDER_COMPLETED, delta = 5, version = 2))

        // assert
        assertThat(model.salesCount).isEqualTo(7)
    }

    @DisplayName("모든 이벤트 타입이 각각의 카운터에 정확하게 반영된다")
    @Test
    fun appliesAllEventTypes() {
        // arrange
        val model = ProductMetricsModel(productId = 1L)

        // act
        model.apply(catalogEvent(eventType = CatalogEventType.LIKE_CHANGED, delta = 1, version = 1))
        model.apply(catalogEvent(eventType = CatalogEventType.PRODUCT_VIEWED, delta = 1, version = 2))
        model.apply(catalogEvent(eventType = CatalogEventType.ORDER_COMPLETED, delta = 1, version = 3))

        // assert
        assertThat(model.likesCount).isEqualTo(1)
        assertThat(model.viewsCount).isEqualTo(1)
        assertThat(model.salesCount).isEqualTo(1)
    }

    private fun catalogEvent(
        eventType: CatalogEventType,
        delta: Long,
        version: Long,
    ): CatalogEventMessage {
        return CatalogEventMessage(
            eventId = "event-$version",
            productId = 1L,
            eventType = eventType,
            delta = delta,
            version = version,
            occurredAt = ZonedDateTime.now(),
        )
    }
}
