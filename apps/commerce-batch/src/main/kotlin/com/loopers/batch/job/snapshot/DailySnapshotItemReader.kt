package com.loopers.batch.job.snapshot

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
    @param:Value("#{jobParameters['targetDate']}") private val targetDate: LocalDate,
) : ItemReader<RankedSnapshot> {

    private var iterator: Iterator<IndexedValue<ZSetEntry>>? = null

    override fun read(): RankedSnapshot? {
        val it = iterator ?: loadEntries().also { iterator = it }
        if (!it.hasNext()) return null
        val (index, entry) = it.next()
        return RankedSnapshot(
            productId = entry.member.toLong(),
            metricDate = targetDate,
            totalScore = entry.score,
            rankPosition = index + 1,
        )
    }

    private fun loadEntries(): Iterator<IndexedValue<ZSetEntry>> {
        val key = RankingKeyGenerator.dailyKey(targetDate)
        return redisZSetTemplate.reverseRangeWithScoresFromMaster(key, 0, -1)
            .withIndex()
            .iterator()
    }
}
