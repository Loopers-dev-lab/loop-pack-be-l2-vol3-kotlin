package com.loopers.infrastructure.scheduler

import com.loopers.domain.queue.QueueRepository
import com.loopers.domain.queue.QueuedUser
import com.loopers.infrastructure.queue.WaitingQueueRegistry
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * 토큰 버킷: 각 큐별 누적 토큰을 추적하여 TPS에 맞춰 정확한 배치 크기 계산
 *
 * 예: TPS=5, fixedDelay=100ms
 * - 100ms마다 0.5개 토큰 누적
 * - 약 2번(200ms) 후 1개 토큰 발급 가능
 * - 10번 반복하면 약 1초에 5명 처리
 */
data class TokenBucket(
    val queueName: String,
    val tpsConfig: Int,
) {
    var accumulatedTokens: Double = 0.0
    var lastProcessedTimeMs: Long = System.currentTimeMillis()

    /**
     * 경과 시간을 기반으로 누적 토큰을 계산하고 발급할 배치 크기 반환
     * @return 발급할 토큰 수 (최소 1)
     */
    fun calculateBatchSize(): Long {
        val currentTimeMs = System.currentTimeMillis()
        val elapsedMs = currentTimeMs - lastProcessedTimeMs

        // TPS를 ms 단위로 변환: TPS / 1000 = 토큰/ms
        val tokensPerMs = tpsConfig / 1000.0
        accumulatedTokens += elapsedMs * tokensPerMs

        lastProcessedTimeMs = currentTimeMs

        // floor(누적 토큰)만큼 발급
        val batchSize = maxOf(1L, accumulatedTokens.toLong())

        // 남은 분수 토큰은 다음 실행을 위해 보존
        accumulatedTokens -= batchSize

        return batchSize
    }

    /**
     * 테스트용: 시간을 경과시키고 배치 크기 계산 (시뮬레이션)
     * @param elapsedMs 경과 시간 (ms)
     */
    internal fun simulateElapsedTimeAndCalculateBatchSize(elapsedMs: Long): Long {
        val tokensPerMs = tpsConfig / 1000.0
        accumulatedTokens += elapsedMs * tokensPerMs

        val batchSize = maxOf(1L, accumulatedTokens.toLong())
        accumulatedTokens -= batchSize

        return batchSize
    }
}

@Component
class QueueScheduler(
    private val queueRepository: QueueRepository,
    private val waitingQueueRegistry: WaitingQueueRegistry,
) {
    private val log = LoggerFactory.getLogger(QueueScheduler::class.java)

    // 각 큐별 토큰 버킷
    private val tokenBuckets = mutableMapOf<String, TokenBucket>()

    @Scheduled(fixedDelay = 100)
    fun processQueue() {
        waitingQueueRegistry.getQueueConfigs().forEach { config ->
            val bucket = tokenBuckets.getOrPut(config.name) {
                TokenBucket(config.name, config.throughputPerServerPerSecond)
            }

            // 토큰 버킷에서 이번 배치의 배치 크기 계산
            val batchSize = bucket.calculateBatchSize()
            val ttlSeconds = config.activeTokenTTLSeconds.toLong()

            runCatching {
                processQueueInternal(config.name, batchSize, ttlSeconds)
            }.onFailure { e ->
                log.error("[QueueScheduler] 처리 중 오류 발생. queueName={}", config.name, e)
            }
        }
    }

    /**
     * 테스트용: 특정 큐의 토큰 버킷을 가져옴
     */
    internal fun getTokenBucket(queueName: String): TokenBucket? = tokenBuckets[queueName]

    private fun processQueueInternal(queueName: String, batchSize: Long, ttlSeconds: Long) {
        val queuedUsers = queueRepository.popMin(queueName, batchSize)
        if (queuedUsers.isEmpty()) return

        // score로 정렬하여 원래 순번 유지
        val sortedQueuedUsers = queuedUsers.sortedBy { it.score }

        // 토큰 발급 시도 및 실패한 사용자 수집 (원래 score 포함)
        val failedUsers = issueTokensAndCollectFailures(queueName, sortedQueuedUsers, ttlSeconds)

        // 실패한 사용자들을 원래 score로 큐에 재삽입 (순번 보존)
        if (failedUsers.isNotEmpty()) {
            log.warn(
                "[QueueScheduler] 토큰 발급 실패 사용자 발생. queueName={}, failedCount={}",
                queueName,
                failedUsers.size,
            )
            reinsertFailedUsers(queueName, failedUsers)
        } else {
            log.debug("[QueueScheduler] 토큰 발급 완료. queueName={}, count={}", queueName, sortedQueuedUsers.size)
        }
    }

    private fun issueTokensAndCollectFailures(
        queueName: String,
        queuedUsers: List<QueuedUser>,
        ttlSeconds: Long,
    ): List<QueuedUser> {
        return queuedUsers.filter { queuedUser ->
            runCatching {
                val token = UUID.randomUUID().toString()
                queueRepository.issueToken(queueName, queuedUser.userId, token, ttlSeconds)
            }.onFailure { e ->
                log.warn(
                    "[QueueScheduler] 토큰 발급 실패. queueName={}, userId={}, score={}",
                    queueName,
                    queuedUser.userId,
                    queuedUser.score,
                    e,
                )
            }.isFailure
        }
    }

    private fun reinsertFailedUsers(queueName: String, failedUsers: List<QueuedUser>) {
        failedUsers.forEach { queuedUser ->
            runCatching {
                // 실패한 사용자를 원래 score(순번 정보)로 다시 삽입
                queueRepository.enter(queueName, queuedUser.userId, queuedUser.score)
                log.debug(
                    "[QueueScheduler] 사용자 재삽입 완료. queueName={}, userId={}, score={}",
                    queueName,
                    queuedUser.userId,
                    queuedUser.score,
                )
            }.onFailure { e ->
                log.error(
                    "[QueueScheduler] 사용자 재삽입 실패. queueName={}, userId={}, score={}",
                    queueName,
                    queuedUser.userId,
                    queuedUser.score,
                    e,
                )
            }
        }
    }
}
