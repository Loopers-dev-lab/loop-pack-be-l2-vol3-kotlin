package com.loopers.batch.job.productmetrics.step

import com.loopers.batch.infrastructure.catalog.ProductMetricsJdbcWriter
import com.loopers.batch.infrastructure.catalog.ProductMetricsRedisReader
import com.loopers.batch.job.productmetrics.ProductMetricsSyncJobConfig
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.LocalDate

@StepScope
@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = ProductMetricsSyncJobConfig.JOB_NAME)
@Component
class ProductMetricsSyncTasklet(
    private val redisReader: ProductMetricsRedisReader,
    private val jdbcWriter: ProductMetricsJdbcWriter,
) : Tasklet {

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val targetDate = resolveTargetDate(chunkContext.stepContext.jobParameters["requestDate"])

        val snapshots = redisReader.readSnapshot(targetDate)
        jdbcWriter.upsertAll(snapshots)
        contribution.incrementWriteCount(snapshots.size.toLong())
        return RepeatStatus.FINISHED
    }

    /**
     * requestDate 잡 파라미터를 LocalDate로 해석한다.
     * - null: 어제 날짜로 폴백
     * - LocalDate: 그대로 사용
     * - String: yyyy-MM-dd로 명시 파싱, 실패 시 Job 중단
     * - 그 외 타입: Job 중단
     *
     * 묵시적 폴백으로 잘못된 날짜가 정상 완료로 기록되는 것을 막기 위해 명시적으로 분기한다.
     */
    private fun resolveTargetDate(rawParam: Any?): LocalDate = when (rawParam) {
        null -> LocalDate.now().minusDays(1)
        is LocalDate -> rawParam
        is String -> runCatching { LocalDate.parse(rawParam) }
            .getOrElse {
                throw IllegalArgumentException(
                    "requestDate 파라미터 형식이 올바르지 않습니다: '$rawParam' (yyyy-MM-dd 필요)", it,
                )
            }
        else -> throw IllegalArgumentException(
            "requestDate 파라미터 타입이 잘못되었습니다: ${rawParam::class.simpleName}",
        )
    }
}
