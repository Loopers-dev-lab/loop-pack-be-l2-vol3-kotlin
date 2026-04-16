package com.loopers.batch.job.ranking.step

import com.loopers.domain.ranking.MvProductRankMonthly
import com.loopers.domain.ranking.MvProductRankRepository
import com.loopers.domain.ranking.MvProductRankWeekly
import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Step 2: 중간 테이블에서 TOP 100 → MV에 새 version INSERT → 중간 테이블 cleanup.
 *
 * 블루-그린 교체: 기존 MV 데이터를 삭제하지 않고, 새 version으로 INSERT한다.
 * API는 최신 완료 version을 조회하므로, 적재 중에도 이전 데이터가 정상 서빙된다.
 */
@StepScope
@Component
class RankAndStoreMvTasklet(
    private val mvProductRankRepository: MvProductRankRepository,
    @param:Value("#{jobParameters['periodType']}") private val periodType: String,
    @param:Value("#{jobParameters['periodKey']}") private val periodKey: String,
) : Tasklet {

    companion object {
        private val log = LoggerFactory.getLogger(RankAndStoreMvTasklet::class.java)
        private const val TOP_N = 100
    }

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val topTemps = mvProductRankRepository.findTopTempByPeriod(periodType, periodKey, TOP_N)

        if (topTemps.isEmpty()) {
            log.warn("[랭킹 MV] 중간 테이블에 데이터 없음 [period={}/{}]", periodType, periodKey)
            return RepeatStatus.FINISHED
        }

        val now = LocalDateTime.now()

        when (periodType) {
            "WEEKLY" -> {
                val currentVersion = mvProductRankRepository.findMaxWeeklyVersion(periodKey) ?: 0
                val newVersion = currentVersion + 1

                val ranks = topTemps.mapIndexed { index, temp ->
                    MvProductRankWeekly(
                        productId = temp.productId,
                        yearWeek = periodKey,
                        rank = index + 1,
                        score = temp.score,
                        likeCount = temp.likeCount,
                        orderCount = temp.orderCount,
                        viewCount = temp.viewCount,
                        version = newVersion,
                        aggregatedAt = now,
                    )
                }

                mvProductRankRepository.saveAllWeekly(ranks)
                log.info(
                    "[랭킹 MV] 주간 TOP {} 적재 완료 [yearWeek={}, version={}]",
                    ranks.size, periodKey, newVersion,
                )
            }
            "MONTHLY" -> {
                val currentVersion = mvProductRankRepository.findMaxMonthlyVersion(periodKey) ?: 0
                val newVersion = currentVersion + 1

                val ranks = topTemps.mapIndexed { index, temp ->
                    MvProductRankMonthly(
                        productId = temp.productId,
                        yearMonth = periodKey,
                        rank = index + 1,
                        score = temp.score,
                        likeCount = temp.likeCount,
                        orderCount = temp.orderCount,
                        viewCount = temp.viewCount,
                        version = newVersion,
                        aggregatedAt = now,
                    )
                }

                mvProductRankRepository.saveAllMonthly(ranks)
                log.info(
                    "[랭킹 MV] 월간 TOP {} 적재 완료 [yearMonth={}, version={}]",
                    ranks.size, periodKey, newVersion,
                )
            }
            else -> log.error("[랭킹 MV] 알 수 없는 periodType: {}", periodType)
        }

        // 중간 테이블 cleanup
        mvProductRankRepository.deleteTempByPeriod(periodType, periodKey)
        log.info("[랭킹 MV] 중간 테이블 cleanup 완료 [period={}/{}]", periodType, periodKey)

        return RepeatStatus.FINISHED
    }
}
