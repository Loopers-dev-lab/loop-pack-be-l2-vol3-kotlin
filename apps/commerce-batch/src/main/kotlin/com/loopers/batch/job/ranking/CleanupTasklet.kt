package com.loopers.batch.job.ranking

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@StepScope
@Component
class CleanupTasklet(
    private val redisTemplate: RedisTemplate<String, String>,
) : Tasklet {

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val tempKey = chunkContext.stepContext.stepExecution.jobExecution.executionContext
            .get("tempKey") as? String

        if (tempKey != null) {
            redisTemplate.delete(tempKey)
        }

        return RepeatStatus.FINISHED
    }
}
