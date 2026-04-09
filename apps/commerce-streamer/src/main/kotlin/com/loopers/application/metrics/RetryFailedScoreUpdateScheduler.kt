package com.loopers.application.metrics

import com.loopers.domain.ranking.repository.FailedScoreUpdateRepository
import com.loopers.domain.ranking.repository.RankingScoreRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class RetryFailedScoreUpdateScheduler(
    private val failedScoreUpdateRepository: FailedScoreUpdateRepository,
    private val rankingScoreRepository: RankingScoreRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${ranking.retry.interval-ms:60000}")
    fun retry() {
        val pendingUpdates = failedScoreUpdateRepository.findPendingUpdates(MAX_RETRY_COUNT, BATCH_SIZE)
        if (pendingUpdates.isEmpty()) return

        log.info("실패 점수 갱신 재처리 시작. 대상 {}건", pendingUpdates.size)

        for (update in pendingUpdates) {
            try {
                rankingScoreRepository.incrementScore(update.productId, update.score, update.eventId, update.rankingDate)
                failedScoreUpdateRepository.delete(update)
                log.info("재처리 성공. eventId={}, productId={}", update.eventId, update.productId)
            } catch (e: Exception) {
                update.incrementRetryCount()
                failedScoreUpdateRepository.save(update)
                log.warn(
                    "재처리 실패 ({}/{}). eventId={}, productId={}: {}",
                    update.retryCount,
                    MAX_RETRY_COUNT,
                    update.eventId,
                    update.productId,
                    e.message,
                )
            }
        }
    }

    companion object {
        private const val MAX_RETRY_COUNT = 10
        private const val BATCH_SIZE = 50
    }
}
