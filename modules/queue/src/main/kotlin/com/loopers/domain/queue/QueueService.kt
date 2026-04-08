package com.loopers.domain.queue

import com.loopers.config.QueueProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil
import kotlin.math.min

@Component
@EnableConfigurationProperties(QueueProperties::class)
class QueueService(
    private val queueRepository: QueueRepository,
    private val queueTokenRepository: QueueTokenRepository,
    private val queueProperties: QueueProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val cycleCounter = AtomicInteger(0)

    fun enterQueue(userId: Long): QueueEntryInfo? {
        val score = System.currentTimeMillis().toDouble()
        val added = queueRepository.addIfAbsent(userId, score)
        if (!added) return null

        val rank = queueRepository.getRank(userId) ?: 0L
        val size = queueRepository.getSize()
        return QueueEntryInfo(position = rank + 1, totalWaiting = size)
    }

    fun getPosition(userId: Long): QueuePositionInfo {
        val rank = queueRepository.getRank(userId)
        if (rank != null) {
            val size = queueRepository.getSize()
            val eta = calculateEta(rank)
            return QueuePositionInfo(
                status = QueueStatus.WAITING,
                position = rank + 1,
                totalWaiting = size,
                estimatedWaitSeconds = eta,
            )
        }

        val token = queueTokenRepository.getToken(userId)
        if (token != null) {
            return QueuePositionInfo(
                status = QueueStatus.TOKEN_ISSUED,
                position = 0,
                totalWaiting = queueRepository.getSize(),
                estimatedWaitSeconds = 0,
                token = token,
            )
        }

        return QueuePositionInfo(
            status = QueueStatus.NOT_IN_QUEUE,
            position = 0,
            totalWaiting = queueRepository.getSize(),
            estimatedWaitSeconds = 0,
        )
    }

    fun popAndIssueTokens(batchSize: Int): Int {
        if (!queueProperties.enabled) return 0
        correctCounterIfNeeded()

        var activeTokens = queueTokenRepository.getActiveTokenCount()
        var available = queueProperties.maxActiveTokens - activeTokens.toInt()

        if (available <= 0) {
            activeTokens = correctCounter()
            available = queueProperties.maxActiveTokens - activeTokens.toInt()
            if (available <= 0) return 0
        }

        val actualBatchSize = min(batchSize, available)
        val userIds = queueRepository.popMin(actualBatchSize.toLong())
        if (userIds.isEmpty()) return 0

        userIds.forEach { userIdStr ->
            queueTokenRepository.issueToken(userIdStr.toLong(), queueProperties.tokenTtlSeconds)
        }
        queueTokenRepository.incrementActiveTokenCount(userIds.size.toLong())
        return userIds.size
    }

    fun validateToken(userId: Long, token: String): Boolean {
        val storedToken = queueTokenRepository.getToken(userId)
        return storedToken != null && storedToken == token
    }

    fun consumeToken(userId: Long) {
        queueTokenRepository.deleteToken(userId)
        queueTokenRepository.decrementActiveTokenCount()
    }

    private fun correctCounterIfNeeded() {
        val cycle = cycleCounter.incrementAndGet()
        if (cycle >= queueProperties.counterCorrectionInterval) {
            cycleCounter.set(0)
            correctCounter()
        }
    }

    private fun correctCounter(): Long {
        val actual = queueTokenRepository.countActiveTokens()
        queueTokenRepository.setActiveTokenCount(actual)
        log.debug("[Queue] 카운터 보정: {}", actual)
        return actual
    }

    private fun calculateEta(rank: Long): Long {
        val batches = ceil((rank + 1).toDouble() / queueProperties.batchSize).toLong()
        val estimatedWaitMs = batches * queueProperties.schedulerIntervalMs
        return estimatedWaitMs / 1000
    }
}
