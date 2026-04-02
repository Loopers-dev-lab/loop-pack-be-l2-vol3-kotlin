package com.loopers.infrastructure.queue

import com.loopers.domain.queue.QueueService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 대기열 스케줄러.
 *
 * 100ms마다 실행하여 대기열에서 N명을 꺼내 입장 토큰을 발급한다.
 * → 1초에 175명을 한 번에 발급하면 Thundering Herd 발생.
 * → 100ms마다 18명씩 나눠 발급하여 부하를 10배 평탄화한다.
 *
 * 다중 인스턴스 환경:
 * - 현재는 단일 인스턴스에서만 활성화 (queue.scheduler.enabled=true).
 * - 실무에서는 배치 서버로 분리하는 것이 일반적.
 *   → Redis 분산 락보다 코드 복잡성이 낮고, 우연한 복잡성(accidental complexity)을 줄일 수 있다.
 */
@Component
@ConditionalOnProperty(name = ["queue.scheduler.enabled"], havingValue = "true", matchIfMissing = true)
class QueueScheduler(
    private val queueService: QueueService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedRate = 100)
    fun issueTokens() {
        try {
            val issued = queueService.processQueue()
            if (issued > 0) {
                log.debug("대기열 토큰 발급: {}명", issued)
            }
        } catch (e: Exception) {
            log.error("대기열 스케줄러 실행 실패", e)
        }
    }
}
