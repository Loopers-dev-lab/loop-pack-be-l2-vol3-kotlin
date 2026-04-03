package com.loopers.application.queue

import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import com.loopers.domain.queue.QueueErrorType
import com.loopers.domain.queue.QueuePosition
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class QueueService(
    private val queueStore: QueueStore,
    private val tokenStore: QueueTokenStore,
    private val configStore: QueueConfigStore,
    @Value("\${queue.batch-size:300}") private val batchSize: Int,
    @Value("\${queue.scheduler-interval-seconds:3}") private val schedulerIntervalSeconds: Int,
    @Value("\${queue.token-ttl-seconds:300}") private val tokenTtlSeconds: Long,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun enterQueue(memberId: Long): QueueInfo {
        if (!configStore.isEnabled()) {
            throw CoreException(ErrorType.BAD_REQUEST, QueueErrorType.QUEUE_DISABLED.message)
        }

        val existingToken = tokenStore.get(memberId)
        if (existingToken != null) {
            return QueueInfo.from(
                QueuePosition(
                    position = 0,
                    totalWaiting = queueStore.size(),
                    batchSize = batchSize,
                    schedulerIntervalSeconds = schedulerIntervalSeconds,
                ),
                token = existingToken,
            )
        }

        val existingRank = queueStore.rank(memberId)
        if (existingRank != null) {
            return QueueInfo.from(
                QueuePosition(
                    position = existingRank + 1,
                    totalWaiting = queueStore.size(),
                    batchSize = batchSize,
                    schedulerIntervalSeconds = schedulerIntervalSeconds,
                ),
            )
        }

        val score = System.currentTimeMillis().toDouble()
        val added = queueStore.add(memberId, score)

        if (!added) {
            val rank = queueStore.rankFromMaster(memberId) ?: 0L
            return QueueInfo.from(
                QueuePosition(
                    position = rank + 1,
                    totalWaiting = queueStore.size(),
                    batchSize = batchSize,
                    schedulerIntervalSeconds = schedulerIntervalSeconds,
                ),
            )
        }

        val rank = queueStore.rankFromMaster(memberId) ?: 0L
        val totalWaiting = queueStore.size()

        return QueueInfo.from(
            QueuePosition(
                position = rank + 1,
                totalWaiting = totalWaiting,
                batchSize = batchSize,
                schedulerIntervalSeconds = schedulerIntervalSeconds,
            ),
        )
    }

    fun getPosition(memberId: Long): QueueInfo {
        val existingToken = tokenStore.get(memberId)
        if (existingToken != null) {
            return QueueInfo.from(
                QueuePosition(
                    position = 0,
                    totalWaiting = queueStore.size(),
                    batchSize = batchSize,
                    schedulerIntervalSeconds = schedulerIntervalSeconds,
                ),
                token = existingToken,
            )
        }

        val rank = queueStore.rank(memberId)
            ?: throw CoreException(ErrorType.NOT_FOUND, QueueErrorType.NOT_IN_QUEUE.message)

        val totalWaiting = queueStore.size()

        return QueueInfo.from(
            QueuePosition(
                position = rank + 1,
                totalWaiting = totalWaiting,
                batchSize = batchSize,
                schedulerIntervalSeconds = schedulerIntervalSeconds,
            ),
        )
    }

    fun validateToken(memberId: Long, token: String): Boolean {
        val storedToken = tokenStore.get(memberId) ?: return false
        return storedToken == token
    }

    fun consumeToken(memberId: Long) {
        tokenStore.delete(memberId)
    }

    fun processQueue(): Int {
        val members = queueStore.popMin(batchSize.toLong())
        if (members.isEmpty()) return 0

        val issued = members.count { memberId ->
            val token = UUID.randomUUID().toString()
            tokenStore.issue(memberId, token, tokenTtlSeconds)
        }

        log.info("[QueueScheduler] 토큰 발급 완료: issued={}, popped={}", issued, members.size)
        return issued
    }

    fun isEnabled(): Boolean = configStore.isEnabled()

    fun setEnabled(enabled: Boolean) = configStore.setEnabled(enabled)

    fun getStatus(): QueueStatusInfo {
        return QueueStatusInfo(
            enabled = configStore.isEnabled(),
            totalWaiting = queueStore.size(),
            activeTokens = tokenStore.activeCount(),
        )
    }
}
