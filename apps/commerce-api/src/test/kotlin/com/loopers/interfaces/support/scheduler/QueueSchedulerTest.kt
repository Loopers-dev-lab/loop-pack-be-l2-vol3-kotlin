package com.loopers.interfaces.support.scheduler

import com.loopers.application.queue.GetQueuePositionUseCase
import com.loopers.application.queue.IssueEntryTokensUseCase
import com.loopers.application.queue.QueueFallbackHandler
import com.loopers.application.queue.QueueProperties
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.queue.token.FakeEntryTokenRepository
import com.loopers.domain.queue.waiting.FakeWaitingQueueRepository
import com.loopers.domain.queue.waiting.repository.WaitingQueueRepository
import com.loopers.interfaces.support.sse.QueueSseEmitterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class QueueSchedulerTest {

    private lateinit var waitingQueueRepository: FakeWaitingQueueRepository
    private lateinit var entryTokenRepository: FakeEntryTokenRepository
    private lateinit var queueFallbackHandler: QueueFallbackHandler
    private lateinit var scheduler: QueueScheduler

    private val properties = QueueProperties(
        maxCapacity = 50_000,
        batchSize = 3,
        tokenTtlSeconds = 300,
        throughputTps = 175,
        schedulerDelayMs = 100,
        jitterMaxMs = 0,
    )

    @BeforeEach
    fun setUp() {
        entryTokenRepository = FakeEntryTokenRepository()
        waitingQueueRepository = FakeWaitingQueueRepository(entryTokenRepository)
        queueFallbackHandler = QueueFallbackHandler()
        val issueUseCase = IssueEntryTokensUseCase(waitingQueueRepository, properties)
        val positionUseCase = GetQueuePositionUseCase(waitingQueueRepository, entryTokenRepository, properties)
        val registry = QueueSseEmitterRegistry(properties)
        scheduler = QueueScheduler(issueUseCase, positionUseCase, registry, queueFallbackHandler)
    }

    @Nested
    @DisplayName("issueTokens 실행 시")
    inner class IssueTokens {

        @Test
        @DisplayName("대기열에서 배치 크기만큼 꺼내 토큰을 발급한다")
        fun issueTokens_popsAndIssuesTokens() {
            // arrange
            waitingQueueRepository.enter(UserId(1L), 1000.0, 50_000)
            waitingQueueRepository.enter(UserId(2L), 2000.0, 50_000)

            // act
            scheduler.issueTokens()

            // assert
            assertThat(entryTokenRepository.find(UserId(1L))).isNotNull()
            assertThat(entryTokenRepository.find(UserId(2L))).isNotNull()
            assertThat(waitingQueueRepository.count()).isEqualTo(0)
        }

        @Test
        @DisplayName("대기열이 비어있으면 아무 동작도 하지 않는다")
        fun issueTokens_emptyQueue_doesNothing() {
            // act
            scheduler.issueTokens()

            // assert
            assertThat(waitingQueueRepository.count()).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("Redis 장애 Fallback 시")
    inner class RedisFallback {

        @Test
        @DisplayName("Redis 장애 시 예외 없이 정상 종료하고 fallback 상태로 전환한다")
        fun issueTokens_redisFailure_marksUnavailable() {
            // arrange
            val failingRepo = object : WaitingQueueRepository {
                override fun enter(userId: UserId, score: Double, maxCapacity: Int): Long? =
                    throw RuntimeException("Redis connection refused")

                override fun findPosition(userId: UserId): Long? =
                    throw RuntimeException("Redis connection refused")

                override fun count(): Long =
                    throw RuntimeException("Redis connection refused")

                override fun popMin(count: Int): List<UserId> =
                    throw RuntimeException("Redis connection refused")

                override fun popMinAndIssueTokens(count: Int, ttlSeconds: Long): List<Pair<UserId, String>> =
                    throw RuntimeException("Redis connection refused")
            }
            val failingIssueUseCase = IssueEntryTokensUseCase(failingRepo, properties)
            val positionUseCase = GetQueuePositionUseCase(failingRepo, entryTokenRepository, properties)
            val registry = QueueSseEmitterRegistry(properties)
            val failingScheduler = QueueScheduler(failingIssueUseCase, positionUseCase, registry, queueFallbackHandler)

            // act — 예외 없이 정상 종료
            failingScheduler.issueTokens()

            // assert
            assertThat(queueFallbackHandler.isAvailable()).isFalse()
        }

        @Test
        @DisplayName("Redis 복구 시 정상 모드로 전환한다")
        fun issueTokens_redisRecovery_marksAvailable() {
            // arrange
            queueFallbackHandler.markUnavailable("Redis down")

            // act — 빈 대기열이지만 Redis 호출 자체는 성공
            scheduler.issueTokens()

            // assert
            assertThat(queueFallbackHandler.isAvailable()).isTrue()
        }
    }
}
