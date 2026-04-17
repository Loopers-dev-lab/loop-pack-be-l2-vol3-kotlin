package com.loopers.batch.retention

import com.loopers.common.DateUtils
import com.loopers.domain.ranking.MonthlyRankRepository
import com.loopers.domain.ranking.WeeklyRankRepository
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class DataRetentionCleanupScheduler(
    private val jdbcTemplate: JdbcTemplate,
    private val weeklyRankRepository: WeeklyRankRepository,
    private val monthlyRankRepository: MonthlyRankRepository,
    private val properties: RetentionProperties,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${batch.retention.cron}", zone = "Asia/Seoul")
    fun cleanup() {
        runStep("daily") { cleanupDaily() }
        runStep("weekly") { cleanupWeekly() }
        runStep("monthly") { cleanupMonthly() }
    }

    fun cleanupDaily(): Int {
        val threshold = DateUtils.todayKst().minusDays(properties.dailyRetentionDays)
        val sql = """
            DELETE FROM product_metrics_daily
            WHERE metric_date < ?
            ORDER BY metric_date
            LIMIT ${properties.batchSize}
        """
        val deleted = batchDelete(sql, java.sql.Date.valueOf(threshold))
        log.info("daily cleanup [thresholdBefore={}, deleted={}]", threshold, deleted)
        return deleted
    }

    fun cleanupWeekly(): Int {
        val threshold = DateUtils.todayKst().minusWeeks(properties.weeklyRetentionWeeks)
        val latest = weeklyRankRepository.findLatestWeekEnd() ?: run {
            log.info("weekly cleanup skip — 데이터 없음")
            return 0
        }
        val sql = """
            DELETE FROM mv_product_rank_weekly
            WHERE week_end < ? AND week_end <> ?
            ORDER BY week_end
            LIMIT ${properties.batchSize}
        """
        val deleted = batchDelete(
            sql,
            java.sql.Date.valueOf(threshold),
            java.sql.Date.valueOf(latest),
        )
        log.info("weekly cleanup [thresholdBefore={}, latestPreserved={}, deleted={}]", threshold, latest, deleted)
        return deleted
    }

    fun cleanupMonthly(): Int {
        val threshold = DateUtils.todayKst().minusMonths(properties.monthlyRetentionMonths)
        val thresholdYearMonth = DateUtils.formatYearMonth(threshold)
        val latest = monthlyRankRepository.findLatestYearMonth() ?: run {
            log.info("monthly cleanup skip — 데이터 없음")
            return 0
        }
        val sql = """
            DELETE FROM mv_product_rank_monthly
            WHERE yearmonth < ? AND yearmonth <> ?
            ORDER BY yearmonth
            LIMIT ${properties.batchSize}
        """
        val deleted = batchDelete(sql, thresholdYearMonth, latest)
        log.info("monthly cleanup [thresholdBefore={}, latestPreserved={}, deleted={}]", thresholdYearMonth, latest, deleted)
        return deleted
    }

    private fun batchDelete(sql: String, vararg args: Any): Int {
        var total = 0
        while (true) {
            val deleted = jdbcTemplate.update(sql, *args)
            total += deleted
            if (deleted < properties.batchSize) break
        }
        return total
    }

    private fun runStep(label: String, block: () -> Int) {
        try {
            block()
        } catch (e: Exception) {
            log.error("retention cleanup 실패 [step={}]", label, e)
        }
    }
}
