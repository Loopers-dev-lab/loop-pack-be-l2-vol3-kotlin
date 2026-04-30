package com.loopers.batch.job.ranking.step

import com.loopers.domain.ranking.MvProductRankMonthly
import com.loopers.domain.ranking.MvProductRankRepository
import com.loopers.domain.ranking.MvProductRankWeekly
import com.loopers.domain.ranking.RankingScoreCalculator
import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Tasklet 방식 — SQL 1발로 집계+rank+적재를 한 번에 처리.
 *
 * Chunk 3-Step의 대안으로, 동일한 결과를 단일 Tasklet에서 생성한다.
 * 중간 테이블 없이 product_metrics → MV 직행.
 *
 * 벤치마크 비교 대상: Chunk(안정성) vs Tasklet(속도)
 */
@StepScope
@Component
class RankingMvTasklet(
    private val jdbcTemplate: JdbcTemplate,
    private val mvProductRankRepository: MvProductRankRepository,
    @param:Value("#{jobParameters['periodType']}") private val periodType: String,
    @param:Value("#{jobParameters['periodKey']}") private val periodKey: String,
    @param:Value("#{jobParameters['startDate']}") private val startDate: String,
    @param:Value("#{jobParameters['endDate']}") private val endDate: String,
    @param:Value("#{jobParameters['viewWeight'] ?: 0.1}") private val viewWeight: Double,
    @param:Value("#{jobParameters['likeWeight'] ?: 0.2}") private val likeWeight: Double,
    @param:Value("#{jobParameters['orderWeight'] ?: 0.7}") private val orderWeight: Double,
) : Tasklet {

    companion object {
        private val log = LoggerFactory.getLogger(RankingMvTasklet::class.java)
        private const val TOP_N = 100
    }

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        // 1. SQL 한 방으로 기간 합산 + score 계산 + TOP 100 조회
        val sql = """
            SELECT product_id,
                   SUM(like_count) as like_count,
                   SUM(order_count) as order_count,
                   SUM(view_count) as view_count
            FROM product_metrics
            WHERE metric_date BETWEEN ? AND ?
            GROUP BY product_id
            ORDER BY (SUM(view_count) * ? + SUM(like_count) * ? + SUM(order_count) * ?) DESC,
                     product_id ASC
            LIMIT ?
        """.trimIndent()

        val rows = jdbcTemplate.query(sql, { rs, _ ->
            Triple(
                rs.getLong("product_id"),
                Triple(rs.getLong("like_count"), rs.getLong("order_count"), rs.getLong("view_count")),
                RankingScoreCalculator.calculate(
                    viewCount = rs.getLong("view_count"),
                    likeCount = rs.getLong("like_count"),
                    orderCount = rs.getLong("order_count"),
                    viewWeight = viewWeight,
                    likeWeight = likeWeight,
                    orderWeight = orderWeight,
                ),
            )
        }, startDate, endDate, viewWeight, likeWeight, orderWeight, TOP_N)

        if (rows.isEmpty()) {
            log.warn("[랭킹 Tasklet] 집계 결과 없음 [period={}/{}]", periodType, periodKey)
            return RepeatStatus.FINISHED
        }

        // 2. 새 version으로 MV 적재
        val now = LocalDateTime.now()

        when (periodType) {
            "WEEKLY" -> {
                val currentVersion = mvProductRankRepository.findMaxWeeklyVersion(periodKey) ?: 0
                val newVersion = currentVersion + 1

                val ranks = rows.mapIndexed { index, (productId, counts, score) ->
                    MvProductRankWeekly(
                        productId = productId,
                        yearWeek = periodKey,
                        rank = index + 1,
                        score = score,
                        likeCount = counts.first,
                        orderCount = counts.second,
                        viewCount = counts.third,
                        version = newVersion,
                        aggregatedAt = now,
                    )
                }
                mvProductRankRepository.saveAllWeekly(ranks)

                // 3. 이전 version 삭제
                mvProductRankRepository.deleteWeeklyByYearWeekAndVersionLessThan(periodKey, newVersion)
                log.info(
                    "[랭킹 Tasklet] 주간 TOP {} 적재 완료 [yearWeek={}, version={}]",
                    ranks.size, periodKey, newVersion,
                )
            }
            "MONTHLY" -> {
                val currentVersion = mvProductRankRepository.findMaxMonthlyVersion(periodKey) ?: 0
                val newVersion = currentVersion + 1

                val ranks = rows.mapIndexed { index, (productId, counts, score) ->
                    MvProductRankMonthly(
                        productId = productId,
                        yearMonth = periodKey,
                        rank = index + 1,
                        score = score,
                        likeCount = counts.first,
                        orderCount = counts.second,
                        viewCount = counts.third,
                        version = newVersion,
                        aggregatedAt = now,
                    )
                }
                mvProductRankRepository.saveAllMonthly(ranks)

                mvProductRankRepository.deleteMonthlyByYearMonthAndVersionLessThan(periodKey, newVersion)
                log.info(
                    "[랭킹 Tasklet] 월간 TOP {} 적재 완료 [yearMonth={}, version={}]",
                    ranks.size, periodKey, newVersion,
                )
            }
            else -> log.error("[랭킹 Tasklet] 알 수 없는 periodType: {}", periodType)
        }

        return RepeatStatus.FINISHED
    }
}
