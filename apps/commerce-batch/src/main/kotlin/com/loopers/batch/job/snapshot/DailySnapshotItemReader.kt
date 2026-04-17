package com.loopers.batch.job.snapshot

import com.loopers.zset.RankingKeyGenerator
import com.loopers.zset.RedisZSetTemplate
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

    private var iterator: Iterator<RankedSnapshot>? = null

    override fun read(): RankedSnapshot? {
        val it = iterator ?: loadAll().also { iterator = it }
        return if (it.hasNext()) it.next() else null
    }

    private fun loadAll(): Iterator<RankedSnapshot> {
        val key = RankingKeyGenerator.dailyKey(targetDate)
        val entries = redisZSetTemplate.reverseRangeWithScores(key, 0, -1)
        return entries.mapIndexed { index, entry ->
            RankedSnapshot(
                productId = entry.member.toLong(),
                metricDate = targetDate,
                totalScore = entry.score,
                rankPosition = index + 1,
            )
        }.iterator()
    }
}
