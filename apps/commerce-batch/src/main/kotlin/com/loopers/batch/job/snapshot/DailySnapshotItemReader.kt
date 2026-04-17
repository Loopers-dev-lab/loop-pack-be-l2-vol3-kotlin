package com.loopers.batch.job.snapshot

import com.loopers.hash.MetricType
import com.loopers.hash.MetricsDailyKey
import com.loopers.hash.RedisHashTemplate
import com.loopers.zset.RankingKeyGenerator
import com.loopers.zset.RedisZSetTemplate
import com.loopers.zset.ZSetEntry
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDate

@StepScope
@Component
class DailySnapshotItemReader(
    private val redisZSetTemplate: RedisZSetTemplate,
    private val redisHashTemplate: RedisHashTemplate,
    @param:Value("#{jobParameters['targetDate']}") private val targetDate: LocalDate,
) : ItemReader<RankedSnapshot> {

    private var iterator: Iterator<IndexedValue<ZSetEntry>>? = null
    private var countsByProduct: Map<Long, Counts> = emptyMap()

    override fun read(): RankedSnapshot? {
        val it = iterator ?: loadEntries().also { iterator = it }
        if (!it.hasNext()) return null
        val (index, entry) = it.next()
        val productId = entry.member.toLong()
        val counts = countsByProduct[productId] ?: Counts.ZERO
        return RankedSnapshot(
            productId = productId,
            metricDate = targetDate,
            viewCount = counts.view,
            likeCount = counts.like,
            orderCount = counts.order,
            totalScore = entry.score,
            rankPosition = index + 1,
        )
    }

    private fun loadEntries(): Iterator<IndexedValue<ZSetEntry>> {
        countsByProduct = loadCounts(MetricsDailyKey.key(targetDate))
        val zsetKey = RankingKeyGenerator.dailyKey(targetDate)
        return redisZSetTemplate.reverseRangeWithScoresFromMaster(zsetKey, 0, -1)
            .withIndex()
            .iterator()
    }

    private fun loadCounts(hashKey: String): Map<Long, Counts> {
        val raw = redisHashTemplate.entriesFromMaster(hashKey)
        if (raw.isEmpty()) return emptyMap()

        val acc = mutableMapOf<Long, MutableCounts>()
        raw.forEach { (field, valueText) ->
            val parsed = MetricsDailyKey.parseField(field) ?: return@forEach
            val value = valueText.toLongOrNull() ?: return@forEach
            val bucket = acc.getOrPut(parsed.first) { MutableCounts() }
            when (parsed.second) {
                MetricType.VIEW -> bucket.view = value
                MetricType.LIKE -> bucket.like = value
                MetricType.ORDER -> bucket.order = value
            }
        }
        return acc.mapValues { (_, m) -> Counts(m.view, m.like, m.order) }
    }

    private data class Counts(val view: Long, val like: Long, val order: Long) {
        companion object {
            val ZERO = Counts(0, 0, 0)
        }
    }

    private class MutableCounts {
        var view: Long = 0
        var like: Long = 0
        var order: Long = 0
    }
}
