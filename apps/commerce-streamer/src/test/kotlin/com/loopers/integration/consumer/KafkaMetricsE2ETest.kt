package com.loopers.integration.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.loopers.domain.metrics.ProductMetricsRepository
import com.loopers.messaging.catalog.CatalogEventMessage
import com.loopers.messaging.catalog.CatalogEventType
import com.loopers.messaging.order.OrderEventMessage
import com.loopers.messaging.order.OrderEventType
import com.loopers.testcontainers.KafkaTestContainersConfig
import com.loopers.testcontainers.RedisTestContainersConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.core.KafkaTemplate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.ZonedDateTime

@SpringBootTest(properties = ["spring.task.scheduling.enabled=false"])
@Import(KafkaTestContainersConfig::class, RedisTestContainersConfig::class)
class KafkaMetricsE2ETest @Autowired constructor(
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
    private val productMetricsRepository: ProductMetricsRepository,
    private val kafkaListenerEndpointRegistry: KafkaListenerEndpointRegistry,
) {
    private val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    private val zoneId: ZoneId = ZoneId.of("Asia/Seoul")

    @BeforeEach
    fun waitForConsumers() {
        waitUntil {
            kafkaListenerEndpointRegistry.listenerContainers.all { it.isRunning }
        }
        Thread.sleep(1_000)
    }

    @Test
    fun `catalog 이벤트를 소비해 like_count와 view_count를 반영한다`() {
        val occurredAt = ZonedDateTime.now(zoneId).truncatedTo(ChronoUnit.SECONDS)
        val metricsDate = occurredAt.toLocalDate()

        kafkaTemplate.send(
            "catalog-events",
            "1",
            objectMapper.readTree(
                objectMapper.writeValueAsString(
                    CatalogEventMessage(
                        eventId = "catalog-e2e-1",
                        eventType = CatalogEventType.PRODUCT_VIEWED,
                        productId = 1L,
                        actorLoginId = null,
                        occurredAt = occurredAt,
                    )
                )
            )
        ).get()

        kafkaTemplate.send(
            "catalog-events",
            "1",
            objectMapper.readTree(
                objectMapper.writeValueAsString(
                    CatalogEventMessage(
                        eventId = "catalog-e2e-2",
                        eventType = CatalogEventType.PRODUCT_LIKED,
                        productId = 1L,
                        actorLoginId = "testuser",
                        occurredAt = occurredAt.plusSeconds(1),
                    )
                )
            )
        ).get()

        waitUntil {
            val metrics = productMetricsRepository.findByProductIdAndMetricsDate(1L, metricsDate)
            metrics != null && metrics.viewCount == 1L && metrics.likeCount == 1L
        }

        val metrics = productMetricsRepository.findByProductIdAndMetricsDate(1L, metricsDate)!!
        assertThat(metrics.viewCount).isEqualTo(1L)
        assertThat(metrics.likeCount).isEqualTo(1L)
        assertThat(metrics.rankingScore).isEqualTo(0.3)
    }

    @Test
    fun `order 이벤트를 소비해 sales_count를 반영한다`() {
        val occurredAt = ZonedDateTime.now(zoneId).truncatedTo(ChronoUnit.SECONDS)
        val metricsDate = occurredAt.toLocalDate()

        kafkaTemplate.send(
            "order-events",
            "100",
            objectMapper.readTree(
                objectMapper.writeValueAsString(
                    OrderEventMessage(
                        eventId = "order-e2e-1",
                        eventType = OrderEventType.PAYMENT_SUCCEEDED,
                        orderId = 100L,
                        paymentId = 10L,
                        userId = 1L,
                        occurredAt = occurredAt,
                        items = listOf(
                            OrderEventMessage.OrderEventItem(productId = 2L, quantity = 3L, unitPrice = 10_000L),
                        ),
                    )
                )
            )
        ).get()

        waitUntil {
            val metrics = productMetricsRepository.findByProductIdAndMetricsDate(2L, metricsDate)
            metrics != null && metrics.salesCount == 3L
        }

        val metrics = productMetricsRepository.findByProductIdAndMetricsDate(2L, metricsDate)!!
        assertThat(metrics.salesCount).isEqualTo(3L)
    }

    private fun waitUntil(condition: () -> Boolean) {
        repeat(20) {
            if (condition()) return
            Thread.sleep(500)
        }
        error("조건이 만족되지 않았습니다.")
    }
}
