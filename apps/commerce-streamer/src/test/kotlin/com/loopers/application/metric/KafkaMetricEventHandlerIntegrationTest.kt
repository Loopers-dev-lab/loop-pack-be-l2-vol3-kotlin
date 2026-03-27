package com.loopers.application.metric

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.loopers.domain.metric.HandledEventRepository
import com.loopers.domain.metric.ProductLikeCountRepository
import com.loopers.domain.metric.ProductMetricRepository
import com.loopers.infrastructure.like.ProductLikeEntity
import com.loopers.infrastructure.like.ProductLikeJpaRepository
import com.loopers.infrastructure.metric.ProductMetricJpaRepository
import com.loopers.infrastructure.metric.HandledEventJpaRepository
import com.loopers.infrastructure.metric.ProcessedPaymentJpaRepository
import com.loopers.infrastructure.outbox.KafkaEventType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource

@DisplayName("KafkaMetricEventHandler integration")
@SpringBootTest(classes = [KafkaMetricEventHandlerIntegrationTest.TestApplication::class])
@ActiveProfiles("test")
@TestPropertySource(
    properties = [
        "datasource.mysql-jpa.main.jdbc-url=jdbc:mysql://localhost:3306/loopers",
        "datasource.mysql-jpa.main.driver-class-name=com.mysql.cj.jdbc.Driver",
        "datasource.mysql-jpa.main.username=application",
        "datasource.mysql-jpa.main.password=application",
        "datasource.redis.master.host=localhost",
        "datasource.redis.master.port=6379",
        "datasource.redis.replicas[0].host=localhost",
        "datasource.redis.replicas[0].port=6380",
    ],
)
class KafkaMetricEventHandlerIntegrationTest
@Autowired
constructor(
    private val kafkaMetricEventHandler: KafkaMetricEventHandler,
    private val productMetricRepository: ProductMetricRepository,
    private val productLikeCountRepository: ProductLikeCountRepository,
    private val handledEventRepository: HandledEventRepository,
    private val handledEventJpaRepository: HandledEventJpaRepository,
    private val processedPaymentJpaRepository: ProcessedPaymentJpaRepository,
    private val productMetricJpaRepository: ProductMetricJpaRepository,
    private val productLikeJpaRepository: ProductLikeJpaRepository,
) {
    private val objectMapper = jacksonObjectMapper()

    @AfterEach
    fun tearDown() {
        handledEventJpaRepository.deleteAll()
        processedPaymentJpaRepository.deleteAll()
        productMetricJpaRepository.deleteAll()
        productLikeJpaRepository.deleteAll()
    }

    @Test
    @DisplayName("두 개의 서로 다른 order event를 역순으로 처리해도 판매 수량이 누락되지 않는다")
    fun handle_orderEvents_outOfOrder_accumulatesUnitsSold() {
        kafkaMetricEventHandler.handle(
            topic = "order-events",
            envelope = paymentSucceededEnvelope(
                eventId = 20L,
                paymentId = 2000L,
                orderId = 200L,
                quantity = 2,
            ),
        )
        kafkaMetricEventHandler.handle(
            topic = "order-events",
            envelope = paymentSucceededEnvelope(
                eventId = 10L,
                paymentId = 2010L,
                orderId = 201L,
                quantity = 1,
            ),
        )

        val metric = productMetricRepository.findByProductId(PRODUCT_ID)
        assertThat(metric?.unitsSold).isEqualTo(3)
        assertThat(handledEventJpaRepository.count()).isEqualTo(2)
        assertThat(handledEventRepository.existsByEventId(10L)).isTrue()
        assertThat(handledEventRepository.existsByEventId(20L)).isTrue()
    }

    @Test
    @DisplayName("중복 delivery는 handled-event와 metric에 한 번만 반영된다")
    fun handle_duplicateDelivery_once() {
        val envelope = paymentSucceededEnvelope(
            eventId = 30L,
            paymentId = 300L,
            orderId = 300L,
            quantity = 4,
        )

        kafkaMetricEventHandler.handle(topic = "order-events", envelope = envelope)
        kafkaMetricEventHandler.handle(topic = "order-events", envelope = envelope)

        val metric = productMetricRepository.findByProductId(PRODUCT_ID)
        assertThat(metric?.unitsSold).isEqualTo(4)
        assertThat(handledEventJpaRepository.count()).isEqualTo(1)
        assertThat(processedPaymentJpaRepository.count()).isEqualTo(1)
    }

    @Test
    @DisplayName("같은 paymentId가 다른 eventId로 다시 와도 판매 수량은 한 번만 반영된다")
    fun handle_duplicatePaymentId_once() {
        kafkaMetricEventHandler.handle(
            topic = "order-events",
            envelope = paymentSucceededEnvelope(
                eventId = 31L,
                paymentId = 301L,
                orderId = 301L,
                quantity = 4,
            ),
        )
        kafkaMetricEventHandler.handle(
            topic = "order-events",
            envelope = paymentSucceededEnvelope(
                eventId = 32L,
                paymentId = 301L,
                orderId = 301L,
                quantity = 4,
            ),
        )

        val metric = productMetricRepository.findByProductId(PRODUCT_ID)
        assertThat(metric?.unitsSold).isEqualTo(4)
        assertThat(handledEventJpaRepository.count()).isEqualTo(2)
        assertThat(processedPaymentJpaRepository.count()).isEqualTo(1)
    }

    @Test
    @DisplayName("like event는 product_like row count를 그대로 product_metrics.like_count에 반영한다")
    fun handle_likeEvent_syncsLikeCountSnapshot() {
        saveLikeRows(1L, 2L, 3L)
        productMetricJpaRepository.saveAndFlush(
            com.loopers.infrastructure.metric.ProductMetricEntity(
                productId = PRODUCT_ID,
                likeCount = 99,
            ),
        )

        kafkaMetricEventHandler.handle(
            topic = "catalog-events",
            envelope = KafkaEventEnvelope(
                eventId = 40L,
                eventType = KafkaEventType.PRODUCT_LIKE_REGISTERED,
                aggregateId = PRODUCT_ID,
                payload = objectMapper.readTree("""{"productId":100}"""),
            ),
        )

        val metric = productMetricRepository.findByProductId(PRODUCT_ID)
        assertThat(metric?.likeCount).isEqualTo(3)
        assertThat(productLikeCountRepository.countByProductId(PRODUCT_ID)).isEqualTo(3)
    }

    private fun saveLikeRows(vararg userIds: Long) {
        userIds.forEach { userId ->
            productLikeJpaRepository.saveAndFlush(
                ProductLikeEntity(
                    userId = userId,
                    productId = PRODUCT_ID,
                ),
            )
        }
    }

    private fun paymentSucceededEnvelope(
        eventId: Long,
        paymentId: Long,
        orderId: Long,
        quantity: Int,
    ): KafkaEventEnvelope =
        KafkaEventEnvelope(
            eventId = eventId,
            eventType = KafkaEventType.PAYMENT_SUCCEEDED,
            aggregateId = orderId,
            payload = objectMapper.readTree(
                """
                {
                  "paymentId": $paymentId,
                  "orderId": $orderId,
                  "userId": 10,
                  "items": [
                    {"productId": $PRODUCT_ID, "quantity": $quantity}
                  ]
                }
                """.trimIndent(),
            ),
        )

    companion object {
        private const val PRODUCT_ID = 100L
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ConfigurationPropertiesScan("com.loopers")
    @ComponentScan(
        basePackages = [
            "com.loopers.application",
            "com.loopers.domain",
            "com.loopers.config",
            "com.loopers.infrastructure",
            "com.loopers.interfaces",
            "com.loopers.support",
        ],
    )
    class TestApplication
}
