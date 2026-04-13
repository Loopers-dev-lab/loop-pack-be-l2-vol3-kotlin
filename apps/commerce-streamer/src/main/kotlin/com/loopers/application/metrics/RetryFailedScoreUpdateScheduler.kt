package com.loopers.application.metrics

import com.loopers.domain.ranking.repository.FailedScoreUpdateRepository
import com.loopers.domain.ranking.repository.RankingScoreRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class RetryFailedScoreUpdateScheduler(
    private val failedScoreUpdateRepository: FailedScoreUpdateRepository,
    private val rankingScoreRepository: RankingScoreRepository,
    @Value("\${ranking.retry.max-count:10}") private val maxRetryCount: Int,
    @Value("\${ranking.retry.batch-size:50}") private val batchSize: Int,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${ranking.retry.interval-ms:60000}")
    fun retry() {
        val pendingUpdates = failedScoreUpdateRepository.findPendingUpdates(maxRetryCount, batchSize)
        if (pendingUpdates.isEmpty()) return

        log.info("실패 점수 갱신 재처리 시작. 대상 {}건", pendingUpdates.size)

        for (update in pendingUpdates) {
            try {
                rankingScoreRepository.incrementScore(update.productId, update.score, update.eventId, update.rankingDate)
            } catch (e: Exception) {
                update.incrementRetryCount()
                failedScoreUpdateRepository.save(update)
                log.warn(
                    "재처리 실패 ({}/{}). eventId={}, productId={}: {}",
                    update.retryCount,
                    maxRetryCount,
                    update.eventId,
                    update.productId,
                    e.message,
                )
                continue
            }
            try {
                failedScoreUpdateRepository.delete(update)
                log.info("재처리 성공. eventId={}, productId={}", update.eventId, update.productId)
            } catch (e: Exception) {
                log.warn(
                    "재처리 레코드 삭제 실패 (다음 스케줄 시 멱등 재처리). eventId={}, productId={}: {}",
                    update.eventId,
                    update.productId,
                    e.message,
                )
            }
        }
    }
}
