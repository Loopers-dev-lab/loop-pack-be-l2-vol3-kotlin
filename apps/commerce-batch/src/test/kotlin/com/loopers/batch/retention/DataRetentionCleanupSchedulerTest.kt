package com.loopers.batch.retention

import com.loopers.common.DateUtils
import com.loopers.domain.ranking.MonthlyRank
import com.loopers.domain.ranking.MonthlyRankRepository
import com.loopers.domain.ranking.ProductMetricsDaily
import com.loopers.domain.ranking.ProductMetricsDailyRepository
import com.loopers.domain.ranking.WeeklyRank
import com.loopers.domain.ranking.WeeklyRankRepository
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
class DataRetentionCleanupSchedulerTest @Autowired constructor(
    private val scheduler: DataRetentionCleanupScheduler,
    private val productMetricsDailyRepository: ProductMetricsDailyRepository,
    private val weeklyRankRepository: WeeklyRankRepository,
    private val monthlyRankRepository: MonthlyRankRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @BeforeEach
    fun setUp() {
        databaseCleanUp.truncateAllTables()
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Test
    fun `daily 테이블에서 보존 기간 90일을 초과한 행만 삭제한다`() {
        val today = DateUtils.todayKst()
        seedDaily(productId = 1L, date = today.minusDays(95))
        seedDaily(productId = 2L, date = today.minusDays(91))
        seedDaily(productId = 3L, date = today.minusDays(89))
        seedDaily(productId = 4L, date = today.minusDays(1))

        val deleted = scheduler.cleanupDaily()

        assertAll(
            { assertThat(deleted).isEqualTo(2) },
            { assertThat(productMetricsDailyRepository.findDailyOrNull(1L, today.minusDays(95))).isNull() },
            { assertThat(productMetricsDailyRepository.findDailyOrNull(2L, today.minusDays(91))).isNull() },
            { assertThat(productMetricsDailyRepository.findDailyOrNull(3L, today.minusDays(89))).isNotNull() },
            { assertThat(productMetricsDailyRepository.findDailyOrNull(4L, today.minusDays(1))).isNotNull() },
        )
    }

    @Test
    fun `weekly 테이블에서 보존 기간 외 행을 삭제하되 최신 week_end는 무조건 보존한다`() {
        val today = DateUtils.todayKst()
        val ancientWeekEnd = today.minusWeeks(20)
        val outOfRangeWeekEnd = today.minusWeeks(15)
        val recentWeekEnd = today.minusWeeks(2)

        seedWeekly(productId = 1L, weekEnd = ancientWeekEnd)
        seedWeekly(productId = 2L, weekEnd = outOfRangeWeekEnd)
        seedWeekly(productId = 3L, weekEnd = recentWeekEnd)

        val deleted = scheduler.cleanupWeekly()

        assertAll(
            { assertThat(deleted).isEqualTo(2) },
            { assertThat(weeklyRankRepository.findRanksByWeekEnd(ancientWeekEnd)).isEmpty() },
            { assertThat(weeklyRankRepository.findRanksByWeekEnd(outOfRangeWeekEnd)).isEmpty() },
            { assertThat(weeklyRankRepository.findRanksByWeekEnd(recentWeekEnd)).hasSize(1) },
        )
    }

    @Test
    fun `weekly의 모든 버전이 보존 기간 외이더라도 최신 버전 한 개는 보존된다`() {
        val today = DateUtils.todayKst()
        val ancientWeekEnd = today.minusWeeks(30)
        val olderButLatestWeekEnd = today.minusWeeks(20)

        seedWeekly(productId = 1L, weekEnd = ancientWeekEnd)
        seedWeekly(productId = 2L, weekEnd = olderButLatestWeekEnd)

        val deleted = scheduler.cleanupWeekly()

        assertAll(
            { assertThat(deleted).isEqualTo(1) },
            { assertThat(weeklyRankRepository.findRanksByWeekEnd(ancientWeekEnd)).isEmpty() },
            { assertThat(weeklyRankRepository.findRanksByWeekEnd(olderButLatestWeekEnd)).hasSize(1) },
            { assertThat(weeklyRankRepository.findLatestWeekEnd()).isEqualTo(olderButLatestWeekEnd) },
        )
    }

    @Test
    fun `monthly 테이블에서 보존 기간 외 행을 삭제하되 최신 yearmonth는 무조건 보존한다`() {
        val today = DateUtils.todayKst()
        val ancientYm = DateUtils.formatYearMonth(today.minusMonths(24))
        val outOfRangeYm = DateUtils.formatYearMonth(today.minusMonths(15))
        val recentYm = DateUtils.formatYearMonth(today.minusMonths(2))

        seedMonthly(productId = 1L, yearMonth = ancientYm)
        seedMonthly(productId = 2L, yearMonth = outOfRangeYm)
        seedMonthly(productId = 3L, yearMonth = recentYm)

        val deleted = scheduler.cleanupMonthly()

        assertAll(
            { assertThat(deleted).isEqualTo(2) },
            { assertThat(monthlyRankRepository.findRanksByYearMonth(ancientYm)).isEmpty() },
            { assertThat(monthlyRankRepository.findRanksByYearMonth(outOfRangeYm)).isEmpty() },
            { assertThat(monthlyRankRepository.findRanksByYearMonth(recentYm)).hasSize(1) },
        )
    }

    @Test
    fun `빈 테이블에서 cleanup을 호출해도 0건 처리로 정상 종료된다`() {
        val daily = scheduler.cleanupDaily()
        val weekly = scheduler.cleanupWeekly()
        val monthly = scheduler.cleanupMonthly()

        assertAll(
            { assertThat(daily).isZero() },
            { assertThat(weekly).isZero() },
            { assertThat(monthly).isZero() },
        )
    }

    private fun seedDaily(productId: Long, date: LocalDate) {
        productMetricsDailyRepository.save(
            ProductMetricsDaily.create(productId = productId, metricDate = date),
        )
    }

    private fun seedWeekly(productId: Long, weekEnd: LocalDate) {
        weeklyRankRepository.save(
            WeeklyRank.create(
                productId = productId,
                weekStart = weekEnd.minusDays(6),
                weekEnd = weekEnd,
                rankPosition = 1,
                totalScore = 10.0,
            ),
        )
    }

    private fun seedMonthly(productId: Long, yearMonth: String) {
        monthlyRankRepository.save(
            MonthlyRank.create(
                productId = productId,
                yearMonth = yearMonth,
                rankPosition = 1,
                totalScore = 10.0,
            ),
        )
    }
}
