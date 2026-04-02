package com.loopers.application.queue

import com.loopers.infrastructure.queue.WaitingQueueRedisRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicLong

@Component
class QueueService(
    private val waitingQueueRedisRepository: WaitingQueueRedisRepository,
    private val queueProperties: QueueProperties,
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val tokenIssuedCount = AtomicLong(0)
    private val tokenConsumedCount = AtomicLong(0)

    fun enterQueue(userId: Long): QueueEntryInfo {
        return try {
            doEnterQueue(userId)
        } catch (e: CoreException) {
            throw e
        } catch (e: Exception) {
            log.error("대기열 진입 중 Redis 장애 발생: userId={}", userId, e)
            handleEnterQueueFallback()
        }
    }

    fun getQueuePosition(userId: Long): QueuePositionInfo {
        return try {
            doGetQueuePosition(userId)
        } catch (e: CoreException) {
            throw e
        } catch (e: Exception) {
            log.error("순번 조회 중 Redis 장애 발생: userId={}", userId, e)
            handleGetPositionFallback()
        }
    }

    fun issueTokens(batchSize: Long): Long {
        return try {
            val issuedUserIds = waitingQueueRedisRepository.popAndIssueTokens(batchSize)
            tokenIssuedCount.addAndGet(issuedUserIds.size.toLong())
            issuedUserIds.size.toLong()
        } catch (e: Exception) {
            log.error("토큰 발급 중 Redis 장애 발생", e)
            0L
        }
    }

    fun validateAndConsumeToken(userId: Long) {
        try {
            val token = waitingQueueRedisRepository.getToken(userId)
                ?: throw CoreException(ErrorType.FORBIDDEN, "입장 토큰이 필요합니다.")
        } catch (e: CoreException) {
            throw e
        } catch (e: Exception) {
            log.error("토큰 검증 중 Redis 장애 발생: userId={}", userId, e)
            handleValidateTokenFallback()
        }
    }

    fun incrementConsumedCount() {
        tokenConsumedCount.incrementAndGet()
    }

    fun getTokenIssuedCount(): Long = tokenIssuedCount.get()

    fun getTokenConsumedCount(): Long = tokenConsumedCount.get()

    fun getTokenExpiryRate(): Double {
        val issued = tokenIssuedCount.get()
        if (issued == 0L) return 0.0
        val consumed = tokenConsumedCount.get()
        return (issued - consumed).toDouble() / issued.toDouble()
    }

    private fun doEnterQueue(userId: Long): QueueEntryInfo {
        if (waitingQueueRedisRepository.hasToken(userId)) {
            return QueueEntryInfo(
                position = 0,
                estimatedWaitSeconds = 0,
                totalWaiting = waitingQueueRedisRepository.getTotalCount(),
            )
        }

        val position = waitingQueueRedisRepository.addToQueue(userId)
        val totalWaiting = waitingQueueRedisRepository.getTotalCount()

        return QueueEntryInfo(
            position = position,
            estimatedWaitSeconds = calculateEstimatedWaitSeconds(position),
            totalWaiting = totalWaiting,
        )
    }

    private fun doGetQueuePosition(userId: Long): QueuePositionInfo {
        val token = waitingQueueRedisRepository.getToken(userId)
        if (token != null) {
            return QueuePositionInfo(
                position = 0,
                estimatedWaitSeconds = 0,
                totalWaiting = waitingQueueRedisRepository.getTotalCount(),
                token = token,
                nextPollAfterMs = 0,
                activateAfterMs = generateJitter(),
            )
        }

        val position = waitingQueueRedisRepository.getPosition(userId)
            ?: return QueuePositionInfo(
                position = 0,
                estimatedWaitSeconds = 0,
                totalWaiting = waitingQueueRedisRepository.getTotalCount(),
                token = null,
                nextPollAfterMs = 0,
                activateAfterMs = null,
            )

        return QueuePositionInfo(
            position = position,
            estimatedWaitSeconds = calculateEstimatedWaitSeconds(position),
            totalWaiting = waitingQueueRedisRepository.getTotalCount(),
            token = null,
            nextPollAfterMs = calculateNextPollInterval(position),
            activateAfterMs = null,
        )
    }

    private fun handleEnterQueueFallback(): QueueEntryInfo {
        return when (queueProperties.fallbackStrategy) {
            QueueFallbackStrategy.BLOCK ->
                throw CoreException(ErrorType.SERVICE_UNAVAILABLE, "대기열 서비스 점검 중입니다. 잠시 후 다시 시도해주세요.")
            QueueFallbackStrategy.BYPASS -> {
                log.warn("BYPASS 모드: 대기열 진입을 우회합니다.")
                QueueEntryInfo(position = 0, estimatedWaitSeconds = 0, totalWaiting = 0)
            }
        }
    }

    private fun handleGetPositionFallback(): QueuePositionInfo {
        return when (queueProperties.fallbackStrategy) {
            QueueFallbackStrategy.BLOCK ->
                throw CoreException(ErrorType.SERVICE_UNAVAILABLE, "대기열 서비스 점검 중입니다. 잠시 후 다시 시도해주세요.")
            QueueFallbackStrategy.BYPASS -> {
                log.warn("BYPASS 모드: 순번 조회를 우회합니다.")
                QueuePositionInfo(
                    position = 0,
                    estimatedWaitSeconds = 0,
                    totalWaiting = 0,
                    token = BYPASS_TOKEN,
                    nextPollAfterMs = 0,
                    activateAfterMs = 0,
                )
            }
        }
    }

    private fun handleValidateTokenFallback() {
        when (queueProperties.fallbackStrategy) {
            QueueFallbackStrategy.BLOCK ->
                throw CoreException(ErrorType.SERVICE_UNAVAILABLE, "대기열 서비스 점검 중입니다. 잠시 후 다시 시도해주세요.")
            QueueFallbackStrategy.BYPASS ->
                log.warn("BYPASS 모드: 토큰 검증을 우회합니다.")
        }
    }

    private fun calculateEstimatedWaitSeconds(position: Long): Long {
        if (position <= 0) return 0
        return position / THROUGHPUT_PER_SECOND
    }

    private fun generateJitter(): Long {
        return ThreadLocalRandom.current().nextLong(0, MAX_JITTER_MS + 1)
    }

    private fun calculateNextPollInterval(position: Long): Long {
        return when {
            position <= 100 -> POLL_INTERVAL_NEAR
            position <= 1000 -> POLL_INTERVAL_MEDIUM
            else -> POLL_INTERVAL_FAR
        }
    }

    companion object {
        private const val THROUGHPUT_PER_SECOND = 175L
        private const val POLL_INTERVAL_NEAR = 1000L
        private const val POLL_INTERVAL_MEDIUM = 3000L
        private const val POLL_INTERVAL_FAR = 5000L
        private const val MAX_JITTER_MS = 2000L
        private const val BYPASS_TOKEN = "BYPASS"
    }
}
