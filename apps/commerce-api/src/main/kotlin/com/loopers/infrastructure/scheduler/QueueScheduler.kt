package com.loopers.infrastructure.scheduler

import com.loopers.domain.queue.QueueRepository
import com.loopers.infrastructure.queue.WaitingQueueRegistry
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class QueueScheduler(
    private val queueRepository: QueueRepository,
    private val waitingQueueRegistry: WaitingQueueRegistry,
) {
    private val log = LoggerFactory.getLogger(QueueScheduler::class.java)

    @Scheduled(fixedDelay = 100)
    fun processQueue() {
        waitingQueueRegistry.getQueueConfigs().forEach { config ->
            val batchSize = maxOf(1L, (config.throughputPerServerPerSecond / 10).toLong())
            val ttlSeconds = config.activeTokenTTLSeconds.toLong()

            runCatching {
                processQueueInternal(config.name, batchSize, ttlSeconds)
            }.onFailure { e ->
                log.error("[QueueScheduler] 처리 중 오류 발생. queueName={}", config.name, e)
            }
        }
    }

    private fun processQueueInternal(queueName: String, batchSize: Long, ttlSeconds: Long) {
        val queuedUsers = queueRepository.popMin(queueName, batchSize)
        if (queuedUsers.isEmpty()) return

        val sortedUserIds = queuedUsers
            .sortedBy { it.score }
            .map { it.userId }

        val failedUserIds = issueTokensAndCollectFailures(queueName, sortedUserIds, ttlSeconds)

        if (failedUserIds.isEmpty()) {
            log.debug("[QueueScheduler] 토큰 발급 완료. queueName={}, count={}", queueName, sortedUserIds.size)
        } else {
            log.error(
                "[QueueScheduler] 토큰 발급 실패 사용자 발생. queueName={}, failedCount={}, userIds={}",
                queueName,
                failedUserIds.size,
                failedUserIds,
            )
        }
    }

    private fun issueTokensAndCollectFailures(
        queueName: String,
        userIds: List<Long>,
        ttlSeconds: Long,
    ): List<Long> {
        return userIds.filter { userId ->
            runCatching {
                val token = UUID.randomUUID().toString()
                queueRepository.issueToken(queueName, userId, token, ttlSeconds)
            }.onFailure { e ->
                log.warn("[QueueScheduler] 토큰 발급 실패. queueName={}, userId={}", queueName, userId, e)
            }.isFailure
        }
    }
}
