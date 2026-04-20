package com.loopers.interfaces.consumer

import com.loopers.testcontainers.MySqlTestContainersConfig
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.Acknowledgment
import java.time.LocalDate
import java.time.ZoneId

@SpringBootTest
@Import(MySqlTestContainersConfig::class)
@MockBean(KafkaTemplate::class)
@DisplayName("ProductMetricsDailyConsumer 통합 테스트")
class ProductMetricsDailyConsumerTest @Autowired constructor(
    private val consumer: ProductMetricsDailyConsumer,
    private val jdbcTemplate: JdbcTemplate,
) {
    private val objectMapper = ObjectMapper()
    private val noopAck = object : Acknowledgment {
        override fun acknowledge() {}
    }

    @AfterEach
    fun tearDown() {
        jdbcTemplate.execute("TRUNCATE TABLE product_metrics_daily")
    }

    private fun buildRecord(
        actionType: String,
        targetId: Long,
        metadata: Map<String, Any> = emptyMap(),
    ): ConsumerRecord<String, ByteArray> {
        val payload = mutableMapOf<String, Any>(
            "actionType" to actionType,
            "targetId" to targetId,
        )
        payload.putAll(metadata)
        return ConsumerRecord("product.action", 0, 0, null, objectMapper.writeValueAsBytes(payload))
    }

    private fun queryMetrics(productId: Long): Map<String, Long>? {
        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
        val results = jdbcTemplate.queryForList(
            "SELECT view_count, like_count, order_count, order_amount_sum FROM product_metrics_daily WHERE product_id = ? AND metric_date = ?",
            productId,
            today,
        )
        if (results.isEmpty()) return null
        val row = results[0]
        return mapOf(
            "view_count" to (row["view_count"] as Number).toLong(),
            "like_count" to (row["like_count"] as Number).toLong(),
            "order_count" to (row["order_count"] as Number).toLong(),
            "order_amount_sum" to (row["order_amount_sum"] as Number).toLong(),
        )
    }

    @Nested
    @DisplayName("단건 이벤트 반영")
    inner class SingleEventReflection {

        @Test
        @DisplayName("VIEW 1건 → view_count=1, 나머지 0")
        fun `VIEW 단건 반영`() {
            // Arrange
            val records = listOf(buildRecord("VIEW", 1001))

            // Act
            consumer.onProductActions(records, noopAck)

            // Assert
            val metrics = queryMetrics(1001)
            assertThat(metrics).isNotNull
            assertThat(metrics!!["view_count"]).isEqualTo(1L)
            assertThat(metrics["like_count"]).isEqualTo(0L)
            assertThat(metrics["order_count"]).isEqualTo(0L)
            assertThat(metrics["order_amount_sum"]).isEqualTo(0L)
        }

        @Test
        @DisplayName("LIKE + ORDER 조합 → like_count=1, order_count=1, order_amount_sum=price*quantity")
        fun `LIKE와 ORDER 복합 반영`() {
            // Arrange
            val records = listOf(
                buildRecord("LIKE", 1002),
                buildRecord("ORDER", 1002, mapOf("price" to 5000L, "quantity" to 3)),
            )

            // Act
            consumer.onProductActions(records, noopAck)

            // Assert
            val metrics = queryMetrics(1002)
            assertThat(metrics).isNotNull
            assertThat(metrics!!["view_count"]).isEqualTo(0L)
            assertThat(metrics["like_count"]).isEqualTo(1L)
            assertThat(metrics["order_count"]).isEqualTo(1L)
            assertThat(metrics["order_amount_sum"]).isEqualTo(15000L)
        }
    }

    @Nested
    @DisplayName("ORDER 정합성 보호")
    inner class OrderIntegrityGuard {

        @Test
        @DisplayName("ORDER 이벤트에 price=null이면 order_count/order_amount_sum 스킵, view/like는 정상 반영")
        fun `ORDER price 누락 시 스킵`() {
            // Arrange
            val records = listOf(
                buildRecord("VIEW", 1003),
                buildRecord("LIKE", 1003),
                buildRecord("ORDER", 1003),
            )

            // Act
            consumer.onProductActions(records, noopAck)

            // Assert
            val metrics = queryMetrics(1003)
            assertThat(metrics).isNotNull
            assertThat(metrics!!["view_count"]).isEqualTo(1L)
            assertThat(metrics["like_count"]).isEqualTo(1L)
            assertThat(metrics["order_count"]).isEqualTo(0L)
            assertThat(metrics["order_amount_sum"]).isEqualTo(0L)
        }
    }

    @Nested
    @DisplayName("배치 집계")
    inner class BatchAggregation {

        @Test
        @DisplayName("같은 productId에 복수 이벤트가 있으면 집계 후 단일 UPSERT row로 저장")
        fun `동일 productId 여러 이벤트 집계`() {
            // Arrange: 상품 1004에 VIEW 3건, LIKE 2건, ORDER 1건
            val records = listOf(
                buildRecord("VIEW", 1004),
                buildRecord("VIEW", 1004),
                buildRecord("VIEW", 1004),
                buildRecord("LIKE", 1004),
                buildRecord("LIKE", 1004),
                buildRecord("ORDER", 1004, mapOf("price" to 10000L, "quantity" to 2)),
            )

            // Act
            consumer.onProductActions(records, noopAck)

            // Assert: DB에 product_metrics_daily row는 1개, 집계 값 확인
            val rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product_metrics_daily WHERE product_id = ?",
                Int::class.java,
                1004L,
            )
            assertThat(rowCount).isEqualTo(1)

            val metrics = queryMetrics(1004)
            assertThat(metrics).isNotNull
            assertThat(metrics!!["view_count"]).isEqualTo(3L)
            assertThat(metrics["like_count"]).isEqualTo(2L)
            assertThat(metrics["order_count"]).isEqualTo(1L)
            assertThat(metrics["order_amount_sum"]).isEqualTo(20000L)
        }
    }
}
