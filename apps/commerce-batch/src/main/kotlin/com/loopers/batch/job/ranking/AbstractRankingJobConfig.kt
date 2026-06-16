package com.loopers.batch.job.ranking

import com.loopers.batch.job.ranking.RankingBatchConstants.CHUNK_SIZE
import com.loopers.batch.job.ranking.RankingBatchConstants.DATE_FORMATTER
import com.loopers.batch.job.ranking.RankingBatchConstants.PRODUCT_METRICS_RANKING_SQL
import com.loopers.batch.job.ranking.step.CleanupRankingTasklet
import com.loopers.batch.job.ranking.step.RankingWriter
import com.loopers.batch.listener.ChunkListener
import com.loopers.batch.listener.JobListener
import com.loopers.batch.listener.StepMonitorListener
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.database.JdbcCursorItemReader
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate
import javax.sql.DataSource

/**
 * Weekly/Monthly 랭킹 잡의 공통 골격.
 *
 * 구현체는 (a) period 경계 계산 함수와 (b) MV 영속화 어댑터 두 가지만 제공한다.
 * Job/Step/Reader/Writer 구성은 베이스가 책임진다.
 */
abstract class AbstractRankingJobConfig<E>(
    protected val jobRepository: JobRepository,
    protected val transactionManager: PlatformTransactionManager,
    protected val jobListener: JobListener,
    protected val stepMonitorListener: StepMonitorListener,
    protected val chunkListener: ChunkListener,
) {
    protected abstract val jobName: String
    protected abstract val stepCleanupName: String
    protected abstract val stepAggregateName: String
    protected abstract val readerName: String
    protected abstract val mvTableName: String

    protected abstract fun startOfPeriod(date: LocalDate): LocalDate
    protected abstract fun endOfPeriod(start: LocalDate): LocalDate

    protected abstract fun deleteByRankingDate(date: LocalDate)
    protected abstract fun saveAll(entities: List<E>)
    protected abstract fun newEntity(row: ProductMetricsRow, rank: Int, date: LocalDate): E

    protected fun buildJob(cleanupStep: Step, aggregateStep: Step): Job {
        return JobBuilder(jobName, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(cleanupStep)
            .next(aggregateStep)
            .listener(jobListener)
            .build()
    }

    protected fun buildCleanupStep(requestDate: String?): Step {
        val start = parseStart(requestDate)
        return StepBuilder(stepCleanupName, jobRepository)
            .tasklet(
                CleanupRankingTasklet(start, ::deleteByRankingDate, mvTableName),
                transactionManager,
            )
            .listener(stepMonitorListener)
            .build()
    }

    protected fun buildAggregateStep(
        requestDate: String?,
        reader: JdbcCursorItemReader<ProductMetricsRow>,
    ): Step {
        val start = parseStart(requestDate)
        val writer = RankingWriter(start, ::newEntity, ::saveAll)
        return StepBuilder(stepAggregateName, jobRepository)
            .chunk<ProductMetricsRow, ProductMetricsRow>(CHUNK_SIZE, transactionManager)
            .reader(reader)
            .writer(writer)
            .listener(writer)
            .listener(stepMonitorListener)
            .listener(chunkListener)
            .build()
    }

    protected fun buildReader(dataSource: DataSource, requestDate: String): JdbcCursorItemReader<ProductMetricsRow> {
        val from = parseStart(requestDate)
        val to = endOfPeriod(from)
        return JdbcCursorItemReaderBuilder<ProductMetricsRow>()
            .name(readerName)
            .dataSource(dataSource)
            .sql(PRODUCT_METRICS_RANKING_SQL)
            .queryArguments(java.sql.Date.valueOf(from), java.sql.Date.valueOf(to))
            .rowMapper { rs, _ ->
                ProductMetricsRow(
                    productId = rs.getLong("product_id"),
                    score = rs.getDouble("score"),
                )
            }
            .build()
    }

    private fun parseStart(requestDate: String?): LocalDate {
        return startOfPeriod(LocalDate.parse(requireNotNull(requestDate), DATE_FORMATTER))
    }
}
