package com.loopers.domain.queue

import com.loopers.domain.queue.dto.QueueEntryInfo
import com.loopers.domain.queue.dto.QueuePositionInfo
import com.loopers.domain.queue.dto.QueueStatusInfo
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service

@Service
class QueueService(
    private val queueRepository: QueueRepository,
) {

    /**
     * NOTE: There is a transient state between popMin (removing from queue) and issueToken (issuing token).
     * During this brief window, rank will be null but token may be issued soon.
     * To handle this, we check token first before rank. A true fix would be to atomically move
     * popMin -> issueToken in the queue implementation.
     */
    fun enter(
        queueName: String,
        userId: Long,
        throughputPerServerPerSecond: Int,
    ): QueueEntryInfo {
        return runCatching {
            // Atomic upsert with monotonically increasing score ensures FIFO ordering
            queueRepository.atomicUpsertWithSequence(queueName, userId)

            // Check token first - may be issued even if rank is not yet populated
            val token = queueRepository.getToken(queueName, userId)
            if (token != null) {
                // Already issued token, return position 0
                return@runCatching QueueEntryInfo(
                    queueName = queueName,
                    userId = userId,
                    position = 0,
                    estimatedWaitSeconds = 0,
                )
            }

            // Token not yet issued, check rank in queue
            val rank = queueRepository.getRank(queueName, userId)
                ?: throw CoreException(ErrorType.QUEUE_NOT_FOUND)
            val position = rank + 1

            QueueEntryInfo(
                queueName = queueName,
                userId = userId,
                position = position,
                estimatedWaitSeconds = estimatedWaitSeconds(position, throughputPerServerPerSecond),
            )
        }.onFailure { e ->
            if (e is CoreException) throw e
            throw CoreException(ErrorType.SERVICE_TEMPORARILY_UNAVAILABLE)
        }.getOrThrow()
    }

    fun getPosition(
        queueName: String,
        userId: Long,
        throughputPerServerPerSecond: Int,
    ): QueuePositionInfo {
        return runCatching {
            val token = queueRepository.getToken(queueName, userId)
            if (token != null) {
                return@runCatching QueuePositionInfo(
                    queueName = queueName,
                    userId = userId,
                    position = 0,
                    estimatedWaitSeconds = 0,
                    token = token,
                )
            }

            // Token not yet issued, retry getting rank in case of transient state
            // between popMin (removing from queue) and issueToken (issuing token)
            val rank = getRankWithRetry(queueName, userId)
                ?: throw CoreException(ErrorType.QUEUE_NOT_FOUND)
            val position = rank + 1

            QueuePositionInfo(
                queueName = queueName,
                userId = userId,
                position = position,
                estimatedWaitSeconds = estimatedWaitSeconds(position, throughputPerServerPerSecond),
            )
        }.onFailure { e ->
            if (e is CoreException) throw e
            throw CoreException(ErrorType.SERVICE_TEMPORARILY_UNAVAILABLE)
        }.getOrThrow()
    }

    /**
     * Retries getting rank with bounded backoff to handle transient state
     * where token is being issued but rank is not yet visible.
     * Max 3 attempts with 10ms backoff.
     */
    private fun getRankWithRetry(queueName: String, userId: Long): Long? {
        val maxAttempts = 3
        val backoffMs = 10L

        repeat(maxAttempts) { attempt ->
            val rank = queueRepository.getRank(queueName, userId)
            if (rank != null) {
                return rank
            }

            if (attempt < maxAttempts - 1) {
                Thread.sleep(backoffMs)
            }
        }

        return null
    }

    private fun estimatedWaitSeconds(position: Long, throughputPerSecond: Int): Long =
        if (throughputPerSecond > 0) position / throughputPerSecond.toLong() else Long.MAX_VALUE

    fun getStatus(
        queueName: String,
        throughputPerServerPerSecond: Int,
    ): QueueStatusInfo {
        return runCatching {
            val totalWaiting = queueRepository.size(queueName)

            QueueStatusInfo(
                queueName = queueName,
                totalWaiting = totalWaiting,
                throughputPerSecond = throughputPerServerPerSecond.toLong(),
            )
        }.onFailure { e ->
            if (e is CoreException) throw e
            throw CoreException(ErrorType.SERVICE_TEMPORARILY_UNAVAILABLE)
        }.getOrThrow()
    }
}
