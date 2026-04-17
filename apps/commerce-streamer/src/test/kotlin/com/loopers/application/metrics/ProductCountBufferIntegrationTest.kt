package com.loopers.application.metrics

import com.loopers.application.ranking.RankingScoreEvent
import com.loopers.common.DateUtils
import com.loopers.hash.MetricType
import com.loopers.hash.MetricsDailyKey
import com.loopers.hash.RedisHashTemplate
import com.loopers.testcontainers.KafkaTestContainersConfig
import com.loopers.testcontainers.MySqlTestContainersConfig
import com.loopers.testcontainers.RedisTestContainersConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(MySqlTestContainersConfig::class, RedisTestContainersConfig::class, KafkaTestContainersConfig::class)
class ProductCountBufferIntegrationTest @Autowired constructor(
    private val eventPublisher: ApplicationEventPublisher,
    private val productCountBuffer: ProductCountBuffer,
    private val productCountFlushScheduler: ProductCountFlushScheduler,
    private val redisHashTemplate: RedisHashTemplate,
) {

    private val todayKey get() = MetricsDailyKey.key(DateUtils.todayKst())

    @BeforeEach
    fun setUp() {
        productCountBuffer.drainAll()
        redisHashTemplate.delete(todayKey)
    }

    @Test
    fun `VIEW 이벤트가 누적 flush되면 Hash의 productId view 필드에 반영된다`() {
        repeat(3) { eventPublisher.publishEvent(RankingScoreEvent.ProductViewed(1L)) }

        productCountFlushScheduler.flush()

        val entries = redisHashTemplate.entriesFromMaster(todayKey)
        assertThat(entries[MetricsDailyKey.field(1L, MetricType.VIEW)]).isEqualTo("3")
    }

    @Test
    fun `LIKE 추가와 취소 이벤트는 순증분으로 Hash에 반영된다`() {
        eventPublisher.publishEvent(RankingScoreEvent.LikeAdded(10L))
        eventPublisher.publishEvent(RankingScoreEvent.LikeAdded(10L))
        eventPublisher.publishEvent(RankingScoreEvent.LikeCancelled(10L))

        productCountFlushScheduler.flush()

        val entries = redisHashTemplate.entriesFromMaster(todayKey)
        assertThat(entries[MetricsDailyKey.field(10L, MetricType.LIKE)]).isEqualTo("1")
    }

    @Test
    fun `OrderCreated 이벤트는 productIds의 각 상품에 ORDER 카운트를 증가시킨다`() {
        eventPublisher.publishEvent(RankingScoreEvent.OrderCreated(listOf(100L, 200L), 10_000L))

        productCountFlushScheduler.flush()

        val entries = redisHashTemplate.entriesFromMaster(todayKey)
        assertAll(
            { assertThat(entries[MetricsDailyKey.field(100L, MetricType.ORDER)]).isEqualTo("1") },
            { assertThat(entries[MetricsDailyKey.field(200L, MetricType.ORDER)]).isEqualTo("1") },
        )
    }

    @Test
    fun `서버 종료 훅이 실행되면 남은 버퍼가 flush된다`() {
        productCountBuffer.add(999L, MetricType.VIEW, 5L)
        productCountBuffer.add(999L, MetricType.ORDER, 2L)

        productCountFlushScheduler.onShutdown()

        val entries = redisHashTemplate.entriesFromMaster(todayKey)
        assertAll(
            { assertThat(entries[MetricsDailyKey.field(999L, MetricType.VIEW)]).isEqualTo("5") },
            { assertThat(entries[MetricsDailyKey.field(999L, MetricType.ORDER)]).isEqualTo("2") },
        )
    }
}
