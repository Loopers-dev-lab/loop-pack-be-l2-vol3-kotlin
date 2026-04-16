package com.loopers.batch.job.ranking

import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.StepExecutionListener
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemReader
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@StepScope
@Component
class RankingChunkReader(
    private val redisTemplate: RedisTemplate<String, String>,
) : ItemReader<RankingItem>, StepExecutionListener {

    private lateinit var items: List<RankingItem>
    private var currentIndex = 0
    private lateinit var tempKey: String

    override fun beforeStep(stepExecution: StepExecution) {
        tempKey = stepExecution.jobExecution.executionContext.getString("tempKey") ?: ""
        initializeItems()
    }

    override fun read(): RankingItem? {
        if (currentIndex >= items.size) {
            return null
        }
        return items[currentIndex++]
    }

    override fun afterStep(stepExecution: StepExecution): ExitStatus = ExitStatus.COMPLETED

    private fun initializeItems() {
        val results = redisTemplate.opsForZSet()
            .reverseRangeWithScores(tempKey, 0, 99)
            ?: run {
                items = emptyList()
                return
            }

        items = results.mapIndexedNotNull { index, tuple ->
            val productId = tuple.value?.toLongOrNull() ?: return@mapIndexedNotNull null
            val score = tuple.score ?: 0.0
            RankingItem(
                productId = productId,
                score = score,
                itemIndex = index,
            )
        }
    }
}

data class RankingItem(
    val productId: Long,
    val score: Double,
    val itemIndex: Int,
)
