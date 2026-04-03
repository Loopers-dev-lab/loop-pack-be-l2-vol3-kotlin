package com.loopers.application.queue

import com.loopers.domain.queue.EntryTokenRepository
import com.loopers.domain.queue.WaitingQueueRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class QueueEntryScheduler(
    private val waitingQueueRepository: WaitingQueueRepository,
    private val entryTokenRepository: EntryTokenRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        /**
         * 배치 크기 산정 근거:
         * - DB 커넥션 풀: 50
         * - 주문 1건 평균 처리 시간: 200ms
         * - 이론적 최대 TPS: 50 / 0.2 = 250 TPS
         * - 안전 마진 70%: 175 TPS
         * - 스케줄러 주기: 100ms
         * - 배치 크기: 175 * 0.1 ≈ 18명
         */
        const val BATCH_SIZE = 18L
        const val TOKEN_TTL_SECONDS = 300L
    }

    @Scheduled(fixedDelay = 100)
    fun processQueue() {
        val userIds = waitingQueueRepository.dequeueTopN(BATCH_SIZE)
        if (userIds.isEmpty()) return

        for (userId in userIds) {
            val token = UUID.randomUUID().toString()
            entryTokenRepository.issueToken(userId, token, TOKEN_TTL_SECONDS)
        }

        log.debug("대기열 입장 토큰 발급 완료. count={}", userIds.size)
    }
}
