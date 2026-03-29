package com.loopers.application

import com.loopers.domain.metrics.ProductMetricsRepository
import com.loopers.event.EventEnvelope
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant

@SpringBootTest
@DisplayName("교차 스트림 version 회귀 테스트")
class CrossStreamVersionIntegrationTest @Autowired constructor(
    private val catalogEventProcessor: CatalogEventProcessor,
    private val orderEventProcessor: OrderEventProcessor,
    private val productMetricsRepository: ProductMetricsRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun catalogEnvelope(
        eventId: String,
        eventType: String = "LIKED",
        productId: Long = 100L,
        version: Long,
    ) = EventEnvelope(
        eventId = eventId,
        eventType = eventType,
        aggregateId = productId.toString(),
        version = version,
        timestamp = Instant.now(),
        payload = """{"userId":1,"productId":$productId}""",
    )

    private fun orderEnvelope(
        eventId: String,
        productId: Long = 100L,
        quantity: Int = 1,
        version: Long,
    ) = EventEnvelope(
        eventId = eventId,
        eventType = "ORDER_COMPLETED",
        aggregateId = "1",
        version = version,
        timestamp = Instant.now(),
        payload = """{"orderId":1,"userId":1,"items":[{"productId":$productId,"quantity":$quantity,"productName":"상품"}],"couponId":null,"totalAmount":10000,"paymentAmount":10000}""",
    )

    @Test
    @DisplayName("order 이벤트(높은 version) 반영 후 catalog 이벤트(낮은 version)가 정상 반영된다.")
    fun catalogEventNotBlockedByOrderVersion() {
        // arrange — order 이벤트가 먼저 처리됨 (version=1000)
        orderEventProcessor.process(orderEnvelope("evt-order-1", version = 1000L))

        // act — catalog 이벤트가 더 낮은 version(10)으로 도착
        catalogEventProcessor.process(catalogEnvelope("evt-like-1", version = 10L))

        // assert — catalog version이 order에 의해 오염되지 않아 정상 반영됨
        val metrics = productMetricsRepository.findByProductId(100L)
        assertThat(metrics).isNotNull
        assertThat(metrics!!.salesCount).isEqualTo(1)
        assertThat(metrics.likeCount).isEqualTo(1)
        assertThat(metrics.version).isEqualTo(10L)
    }

    @Test
    @DisplayName("catalog → order → catalog 순서로 처리해도 모든 이벤트가 정상 반영된다.")
    fun interleavedEventsAllProcessed() {
        // act
        catalogEventProcessor.process(catalogEnvelope("evt-like-1", version = 5L))
        orderEventProcessor.process(orderEnvelope("evt-order-1", quantity = 3, version = 9999L))
        catalogEventProcessor.process(catalogEnvelope("evt-view-1", eventType = "VIEWED", version = 10L))

        // assert
        val metrics = productMetricsRepository.findByProductId(100L)
        assertThat(metrics).isNotNull
        assertThat(metrics!!.likeCount).isEqualTo(1)
        assertThat(metrics.salesCount).isEqualTo(3)
        assertThat(metrics.viewCount).isEqualTo(1)
        assertThat(metrics.version).isEqualTo(10L)
    }
}
