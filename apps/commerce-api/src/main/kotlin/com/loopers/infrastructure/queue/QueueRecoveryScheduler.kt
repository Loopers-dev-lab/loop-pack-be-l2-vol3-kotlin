package com.loopers.infrastructure.queue

import com.loopers.domain.queue.QueueService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 대기열 복구 스케줄러.
 *
 * processQueue() 실행 중 서버가 죽으면 {queue}:processing에 항목이 남아있을 수 있음.
 * (SQS Visibility Timeout과 동일한 원리)
 *
 * 10초마다 처리 중 상태가 만료된 항목을 감지해 waiting으로 복구한다.
 * - 대기열 비활성화(isQueueEnabled=false) 시에는 실행하지 않음.
 * - processingTimeoutSeconds(기본 30초) 이상 processing에 머문 항목을 복구 대상으로 판단.
 */
@Component
@ConditionalOnProperty(name = ["queue.scheduler.enabled"], havingValue = "true", matchIfMissing = true)
class QueueRecoveryScheduler(
    private val queueService: QueueService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedRate = 10_000)
    fun recoverExpiredProcessing() {
        try {
            val recovered = queueService.recoverExpiredProcessing()
            if (recovered > 0) {
                log.warn("처리 중 만료 항목 복구 완료: {}명", recovered)
            }
        } catch (e: Exception) {
            log.error("대기열 복구 스케줄러 실행 실패", e)
        }
    }
}
