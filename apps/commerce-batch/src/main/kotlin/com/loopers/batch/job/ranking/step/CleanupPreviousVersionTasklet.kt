package com.loopers.batch.job.ranking.step

import com.loopers.domain.ranking.MvProductRankRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Step 3: 이전 version 데이터 삭제.
 *
 * Step 2에서 새 version이 완전히 적재된 후에 실행된다.
 * 이 Step이 실패해도 API에는 영향 없다 (이전 데이터가 남아있을 뿐).
 */
@StepScope
@Component
class CleanupPreviousVersionTasklet(
    private val mvProductRankRepository: MvProductRankRepository,
    @param:Value("#{jobParameters['periodType']}") private val periodType: String,
    @param:Value("#{jobParameters['periodKey']}") private val periodKey: String,
) : Tasklet {

    companion object {
        private val log = LoggerFactory.getLogger(CleanupPreviousVersionTasklet::class.java)
    }

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        when (periodType) {
            "WEEKLY" -> {
                val maxVersion = mvProductRankRepository.findMaxWeeklyVersion(periodKey) ?: return RepeatStatus.FINISHED
                mvProductRankRepository.deleteWeeklyByYearWeekAndVersionLessThan(periodKey, maxVersion)
                log.info("[랭킹 cleanup] 주간 이전 버전 삭제 [yearWeek={}, maxVersion={}]", periodKey, maxVersion)
            }
            "MONTHLY" -> {
                val maxVersion = mvProductRankRepository.findMaxMonthlyVersion(periodKey) ?: return RepeatStatus.FINISHED
                mvProductRankRepository.deleteMonthlyByYearMonthAndVersionLessThan(periodKey, maxVersion)
                log.info("[랭킹 cleanup] 월간 이전 버전 삭제 [yearMonth={}, maxVersion={}]", periodKey, maxVersion)
            }
            else -> log.error("[랭킹 cleanup] 알 수 없는 periodType: {}", periodType)
        }

        return RepeatStatus.FINISHED
    }
}
