package com.loopers.batch.consistency

import com.loopers.domain.ranking.ProductMetricsDaily
import com.loopers.domain.ranking.ProductMetricsDailyRepository
import com.loopers.hash.MetricType
import com.loopers.hash.MetricsDailyKey
import com.loopers.hash.RedisHashTemplate
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest
class RankingConsistencyCheckSchedulerTest @Autowired constructor(
    private val scheduler: RankingConsistencyCheckScheduler,
    private val redisHashTemplate: RedisHashTemplate,
    private val productMetricsDailyRepository: ProductMetricsDailyRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    private val targetDate: LocalDate = LocalDate.of(2026, 5, 1)

    @BeforeEach
    fun setUp() {
        databaseCleanUp.truncateAllTables()
        redisHashTemplate.delete(MetricsDailyKey.key(targetDate))
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisHashTemplate.delete(MetricsDailyKey.key(targetDate))
    }

    @Test
    fun `Redis와 DB가 모두 일치하면 drift 0건이다`() {
        seedHash(productId = 1L, view = 100L, like = 30L, order = 5L)
        seedDailyRow(productId = 1L, view = 100L, like = 30L, order = 5L)

        val result = scheduler.runCheck(targetDate)

        assertAll(
            { assertThat(result.driftCount).isZero() },
            { assertThat(result.drifts).isEmpty() },
        )
    }

    @Test
    fun `Redis와 DB의 view 카운트가 다르면 drift로 감지한다`() {
        seedHash(productId = 1L, view = 200L, like = 30L, order = 5L)
        seedDailyRow(productId = 1L, view = 100L, like = 30L, order = 5L)

        val result = scheduler.runCheck(targetDate)

        assertAll(
            { assertThat(result.driftCount).isEqualTo(1) },
            { assertThat(result.drifts.first().productId).isEqualTo(1L) },
            { assertThat(result.drifts.first().type).isEqualTo(MetricType.VIEW) },
            { assertThat(result.drifts.first().redisValue).isEqualTo(200L) },
            { assertThat(result.drifts.first().dbValue).isEqualTo(100L) },
        )
    }

    @Test
    fun `Redis에만 있는 productId의 카운트는 DB 0으로 간주해 drift로 감지된다`() {
        seedHash(productId = 99L, view = 50L)

        val result = scheduler.runCheck(targetDate)

        assertAll(
            { assertThat(result.driftCount).isEqualTo(1) },
            { assertThat(result.drifts.first().redisValue).isEqualTo(50L) },
            { assertThat(result.drifts.first().dbValue).isZero() },
        )
    }

    @Test
    fun `DB에만 있는 productId의 카운트는 Redis 0으로 간주해 drift로 감지된다`() {
        seedDailyRow(productId = 7L, view = 30L, like = 10L)

        val result = scheduler.runCheck(targetDate)

        assertAll(
            { assertThat(result.driftCount).isEqualTo(2) },
            { assertThat(result.drifts.map { it.dbValue }).containsExactlyInAnyOrder(30L, 10L) },
            { assertThat(result.drifts).allMatch { it.redisValue == 0L } },
        )
    }

    @Test
    fun `Redis와 DB가 모두 비어 있으면 drift 0건으로 정상 종료된다`() {
        val result = scheduler.runCheck(targetDate)

        assertAll(
            { assertThat(result.driftCount).isZero() },
            { assertThat(result.drifts).isEmpty() },
        )
    }

    private fun seedHash(productId: Long, view: Long = 0L, like: Long = 0L, order: Long = 0L) {
        val key = MetricsDailyKey.key(targetDate)
        if (view > 0L) redisHashTemplate.increment(key, MetricsDailyKey.field(productId, MetricType.VIEW), view)
        if (like > 0L) redisHashTemplate.increment(key, MetricsDailyKey.field(productId, MetricType.LIKE), like)
        if (order > 0L) redisHashTemplate.increment(key, MetricsDailyKey.field(productId, MetricType.ORDER), order)
    }

    private fun seedDailyRow(productId: Long, view: Long = 0L, like: Long = 0L, order: Long = 0L) {
        productMetricsDailyRepository.save(
            ProductMetricsDaily.create(
                productId = productId,
                metricDate = targetDate,
                viewCount = view,
                likeCount = like,
                orderCount = order,
            ),
        )
    }
}
