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

    fun enter(
        queueName: String,
        userId: Long,
        throughputPerServerPerSecond: Int,
    ): QueueEntryInfo {
        return runCatching {
            // Atomic upsert with monotonically increasing score ensures FIFO ordering
            queueRepository.atomicUpsertWithSequence(queueName, userId)

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

            val rank = queueRepository.getRank(queueName, userId)
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
