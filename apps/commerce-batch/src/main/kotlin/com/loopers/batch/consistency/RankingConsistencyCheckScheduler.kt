package com.loopers.batch.consistency

import com.loopers.common.DateUtils
import com.loopers.domain.ranking.ProductMetricsDailyRepository
import com.loopers.hash.MetricType
import com.loopers.hash.MetricsDailyKey
import com.loopers.hash.RedisHashTemplate
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class RankingConsistencyCheckScheduler(
    private val redisHashTemplate: RedisHashTemplate,
    private val productMetricsDailyRepository: ProductMetricsDailyRepository,
    private val properties: ConsistencyCheckProperties,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${batch.consistency.cron}", zone = "Asia/Seoul")
    fun check() {
        val date = DateUtils.yesterdayKst()
        try {
            runCheck(date)
        } catch (e: Exception) {
            log.error("정합성 검증 실패 [date={}]", date, e)
        }
    }

    fun runCheck(date: LocalDate): CheckResult {
        val redisCounts = loadRedisCounts(date)
        val dbCounts = loadDbCounts(date)
        val drifts = calculateDrifts(redisCounts, dbCounts)

        val sampleText = drifts.take(SAMPLE_SIZE).joinToString { d ->
            "(productId=${d.productId}, type=${d.type.code}, redis=${d.redisValue}, db=${d.dbValue})"
        }

        when {
            drifts.isEmpty() ->
                log.info("정합성 검증 정상 [date={}, redisFields={}, dbRows={}]", date, redisCounts.size, dbCounts.size / 3)
            drifts.size <= properties.warnThreshold ->
                log.warn("정합성 drift 감지 (소수, 자동 보정 없음) [date={}, count={}, samples={}]", date, drifts.size, sampleText)
            else ->
                log.error("정합성 drift 대량 발생 — 수동 확인 필요 [date={}, count={}, samples={}]", date, drifts.size, sampleText)
        }

        return CheckResult(date, drifts.size, drifts)
    }

    private fun loadRedisCounts(date: LocalDate): Map<MetricKey, Long> {
        val key = MetricsDailyKey.key(date)
        return redisHashTemplate.entriesFromMaster(key).mapNotNull { (field, valueText) ->
            val parsed = MetricsDailyKey.parseField(field) ?: return@mapNotNull null
            val value = valueText.toLongOrNull() ?: return@mapNotNull null
            MetricKey(parsed.first, parsed.second) to value
        }.toMap()
    }

    private fun loadDbCounts(date: LocalDate): Map<MetricKey, Long> {
        val rows = productMetricsDailyRepository.findAllDailyOn(date)
        val result = mutableMapOf<MetricKey, Long>()
        rows.forEach { row ->
            result[MetricKey(row.productId, MetricType.VIEW)] = row.viewCount
            result[MetricKey(row.productId, MetricType.LIKE)] = row.likeCount
            result[MetricKey(row.productId, MetricType.ORDER)] = row.orderCount
        }
        return result
    }

    private fun calculateDrifts(
        redis: Map<MetricKey, Long>,
        db: Map<MetricKey, Long>,
    ): List<Drift> {
        val keys = redis.keys + db.keys
        return keys.mapNotNull { key ->
            val r = redis[key] ?: 0L
            val d = db[key] ?: 0L
            if (r != d) Drift(key.productId, key.type, r, d) else null
        }
    }

    data class MetricKey(val productId: Long, val type: MetricType)
    data class Drift(val productId: Long, val type: MetricType, val redisValue: Long, val dbValue: Long)
    data class CheckResult(val date: LocalDate, val driftCount: Int, val drifts: List<Drift>)

    companion object {
        private const val SAMPLE_SIZE = 5
    }
}
