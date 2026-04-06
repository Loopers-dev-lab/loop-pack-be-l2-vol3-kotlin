package com.loopers.domain.queue

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * 대기열 핵심 비즈니스 로직.
 *
 * 책임:
 * - 대기열 진입 / 순번 조회 / 예상 대기 시간 계산
 * - 스케줄러에 의한 토큰 발급 (processQueue)
 * - 토큰 검증 + 소비 (주문 API 진입 시)
 *
 * 처리량 설계 기준:
 * - DB 커넥션 풀: 40 (HikariCP max)
 * - 주문 1건 평균 처리 시간: ~200ms (재고 차감 + 쿠폰 검증 + 주문 생성)
 * - 이론적 최대 TPS: 40 / 0.2 = 200
 * - 안전 마진 ~87%: 175 TPS
 * - 스케줄러: 100ms마다 18명씩 발급 → 초당 180명 ≈ 175 TPS
 */
@Component
class QueueService(
    private val queueRepository: QueueRepository,
    private val tokenRepository: TokenRepository,
    private val queueProperties: QueueProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 대기열에 진입한다.
     *
     * - 액티브 토큰에 여유가 있으면 큐를 거치지 않고 바로 토큰을 발급한다.
     * - 최대 인원 초과 시 거부한다.
     * - 이미 대기열에 있으면 기존 순번을 반환한다 (ZADD NX).
     */
    fun enterQueue(userId: Long): QueueEntryInfo {
        // 이미 토큰이 발급된 유저인지 확인
        val existingToken = tokenRepository.get(userId)
        if (existingToken != null) {
            return QueueEntryInfo(
                position = 0,
                estimatedWaitSeconds = 0,
                retryAfterMs = 1_000,
                alreadyInQueue = true,
            )
        }

        // 액티브 토큰에 여유가 있으면 바로 토큰 발급
        val activeCount = tokenRepository.activeCount()
        if (activeCount < queueProperties.maxActiveTokens) {
            val token = UUID.randomUUID().toString()
            tokenRepository.save(userId, token, queueProperties.tokenTtlSeconds)
            return QueueEntryInfo(
                position = 0,
                estimatedWaitSeconds = 0,
                retryAfterMs = 1_000,
                alreadyInQueue = false,
            )
        }

        // 최대 인원 체크
        if (queueProperties.maxQueueSize > 0) {
            val currentSize = queueRepository.size()
            if (currentSize >= queueProperties.maxQueueSize) {
                throw CoreException(ErrorType.QUEUE_FULL)
            }
        }

        // 대기열 진입 시도.
        // - 신규 유저: score(진입 시각) 부여 후 추가
        // - 재진입 유저: NX 옵션으로 score 갱신 없이 기존 순번 유지
        val added = queueRepository.add(userId)
        val rank = queueRepository.getRank(userId)
            ?: throw CoreException(ErrorType.INTERNAL_ERROR, "대기열 순번 조회에 실패했습니다.")

        val position = rank + 1 // 0-based → 1-based
        return QueueEntryInfo(
            position = position,
            estimatedWaitSeconds = calculateEstimatedWait(position),
            retryAfterMs = calculateRetryAfterMs(position),
            alreadyInQueue = !added,
        )
    }

    /**
     * 현재 순번과 예상 대기 시간을 조회한다.
     *
     * - 토큰이 이미 발급되었으면 position=0 + 토큰을 응답에 포함한다.
     * - 대기열에 없으면 예외를 던진다.
     */
    fun getPosition(userId: Long): QueuePositionInfo {
        // 토큰이 이미 발급되었는지 확인
        val token = tokenRepository.get(userId)
        if (token != null) {
            return QueuePositionInfo(
                position = 0,
                estimatedWaitSeconds = 0,
                retryAfterMs = 1_000,
                token = token,
                totalWaiting = queueRepository.size(),
            )
        }

        // 대기열에서 순번 조회
        val rank = queueRepository.getRank(userId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "대기열에 등록되지 않은 유저입니다.")

        val position = rank + 1
        val totalWaiting = queueRepository.size()

        return QueuePositionInfo(
            position = position,
            estimatedWaitSeconds = calculateEstimatedWait(position),
            retryAfterMs = calculateRetryAfterMs(position),
            totalWaiting = totalWaiting,
        )
    }

    /**
     * 스케줄러가 호출: 대기열에서 N명을 꺼내 입장 토큰을 발급한다.
     *
     * 대기열 비활성화 시 즉시 반환 (Redis 조회 없음).
     *
     * Staging Area 패턴 (SQS Visibility Timeout과 동일한 원리):
     * 1. moveToProcessing() — waiting → processing 원자적 이동 (Lua)
     * 2. saveAll() — 토큰 발급 성공
     * 3. removeFromProcessing() — processing에서 제거
     *
     * 서버가 2번에서 죽으면 → processing에 남아있음
     * → 복구 스케줄러(recoverExpiredProcessing)가 waiting으로 복구
     */
    fun processQueue(batchSize: Int = queueProperties.batchSize): Int {
        if (!isQueueEnabled()) return 0

        val activeCount = tokenRepository.activeCount()
        val available = (queueProperties.maxActiveTokens - activeCount).coerceAtLeast(0)
        val actualBatch = minOf(batchSize.toLong(), available).toInt()

        if (actualBatch <= 0) return 0

        val entries = queueRepository.moveToProcessing(actualBatch.toLong())
        if (entries.isEmpty()) return 0

        return try {
            val tokens = entries.associate { it.userId to UUID.randomUUID().toString() }
            tokenRepository.saveAll(tokens, queueProperties.tokenTtlSeconds)
            queueRepository.removeFromProcessing(entries.map { it.userId })
            tokens.size
        } catch (e: Exception) {
            // 토큰 발급 실패 → processing에 남겨두고 복구 스케줄러가 재처리
            log.error("토큰 발급 실패, 처리 중 상태 유지하여 복구 예정 (count={})", entries.size, e)
            0
        }
    }

    /**
     * 복구 스케줄러가 호출: 처리 중 상태에서 만료된 항목을 waiting으로 복구한다.
     *
     * 대기열 비활성화 시 즉시 반환.
     */
    fun recoverExpiredProcessing(): Int {
        if (!isQueueEnabled()) return 0
        val beforeTimestampMs = System.currentTimeMillis() - queueProperties.processingTimeoutSeconds * 1000
        val recovered = queueRepository.recoverExpiredToWaiting(beforeTimestampMs)
        if (recovered > 0) {
            log.warn("처리 중 만료된 {}명을 대기열로 복구함", recovered)
        }
        return recovered
    }

    /** 대기열 활성화 여부를 조회한다. */
    fun isQueueEnabled(): Boolean = queueRepository.isEnabled()

    /** 대기열을 활성화한다. */
    fun enableQueue() = queueRepository.enable()

    /** 대기열을 비활성화한다. */
    fun disableQueue() = queueRepository.disable()

    /**
     * 토큰을 검증하고 소비한다 (주문 API 진입 시).
     *
     * Lua 스크립트로 원자적 처리: 토큰 값 비교 + 삭제를 하나의 연산으로.
     * → 동일 유저가 두 탭에서 동시에 주문해도 하나만 통과.
     */
    fun validateAndConsumeToken(userId: Long, token: String) {
        val consumed = tokenRepository.validateAndConsume(userId, token)
        if (!consumed) {
            throw CoreException(ErrorType.INVALID_TOKEN)
        }
    }

    /**
     * 예상 대기 시간 계산.
     * = 내 순번 / 초당 처리량
     */
    private fun calculateEstimatedWait(position: Long): Long {
        if (position <= 0) return 0
        return (position / queueProperties.throughputPerSecond).toLong().coerceAtLeast(1)
    }

    /**
     * 폴링 주기 계산 (순번 구간별).
     *
     * 기준: 스케줄러가 100ms마다 18명씩 토큰 발급 → 초당 약 180명 처리.
     * - position ≤ 100 → 약 0.5초 내 내 차례 → 1초 폴링
     * - position ≤ 1,000 → 약 5~6초 내 내 차례 → 3초 폴링
     * - position > 1,000 → 5초 이상 대기 확실 → 5초 폴링 (서버 부하 절감)
     *
     * 서버가 retryAfterMs를 응답에 포함 → 클라이언트 배포 없이 서버에서만 주기 조절 가능.
     */
    private fun calculateRetryAfterMs(position: Long): Long {
        return when {
            position <= 0 -> 1_000
            position <= 100 -> 1_000
            position <= 1_000 -> 3_000
            else -> 5_000
        }
    }
}
