package com.loopers.infrastructure.outbox

import com.loopers.domain.outbox.OutboxEventRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class OutboxEventCleaner(
    private val outboxEventRepository: OutboxEventRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val RETENTION_DAYS = 7L
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    fun cleanOldSentEvents() {
        val threshold = ZonedDateTime.now().minusDays(RETENTION_DAYS)
        val deletedCount = outboxEventRepository.deleteSentBefore(threshold)
        if (deletedCount > 0) {
            log.info("Outbox 정리 완료: ${deletedCount}건 삭제 (${RETENTION_DAYS}일 이전 SENT)")
        }
    }
}
