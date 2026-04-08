package com.loopers.application.ranking

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class RankingSnapshotScheduler(
    private val getRankingsUseCase: GetRankingsUseCase,
    private val rankingSnapshotStore: RankingSnapshotStore,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedRate = 3_600_000)
    fun saveSnapshot() {
        val today = LocalDate.now()
        try {
            val rankings = getRankingsUseCase.execute(today, 0, SNAPSHOT_SIZE)
            rankingSnapshotStore.save(today, rankings)
            log.info("랭킹 스냅샷 저장 [date={}, count={}]", today, rankings.content.size)
        } catch (e: Exception) {
            log.warn("랭킹 스냅샷 저장 실패 [date={}]", today, e)
        }
    }

    companion object {
        private const val SNAPSHOT_SIZE = 100
    }
}
