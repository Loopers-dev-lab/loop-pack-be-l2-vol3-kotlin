package com.loopers.application.queue

import com.loopers.domain.queue.EntryTokenRepository
import com.loopers.domain.queue.QueueThroughput
import com.loopers.domain.queue.WaitingQueueRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.UUID

// 테스트 프로파일에서는 백그라운드 폴링이 공유 Redis의 대기열을 오염시키므로 끈다.
// (apps/commerce-api/src/test/resources/application-test.yml)
@ConditionalOnProperty(name = ["queue.scheduler.enabled"], havingValue = "true", matchIfMissing = true)
@Component
class QueueEntryScheduler(
    private val waitingQueueRepository: WaitingQueueRepository,
    private val entryTokenRepository: EntryTokenRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val TOKEN_TTL_SECONDS = 300L
    }

    // fixedDelay가 아니라 fixedRate인 이유: 설계 처리량 140 TPS는 100ms "주기"를 전제로
    // 산정했다 (QueueThroughput). fixedDelay는 실행 시간이 주기에 가산되어 처리량이
    // 항상 설계값 밑으로 떨어진다.
    @Scheduled(fixedRate = QueueThroughput.SCHEDULER_INTERVAL_MS)
    fun processQueue() {
        val userIds = waitingQueueRepository.dequeueTopN(QueueThroughput.BATCH_SIZE)
        if (userIds.isEmpty()) return

        for (userId in userIds) {
            val token = UUID.randomUUID().toString()
            entryTokenRepository.issueToken(userId, token, TOKEN_TTL_SECONDS)
        }

        log.debug("대기열 입장 토큰 발급 완료. count={}", userIds.size)
    }
}
