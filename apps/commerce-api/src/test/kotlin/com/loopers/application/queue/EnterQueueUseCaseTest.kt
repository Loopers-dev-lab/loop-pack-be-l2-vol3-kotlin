package com.loopers.application.queue

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.queue.token.FakeEntryTokenRepository
import com.loopers.domain.queue.token.repository.EntryTokenRepository
import com.loopers.domain.queue.waiting.FakeWaitingQueueRepository
import com.loopers.domain.queue.waiting.repository.WaitingQueueRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EnterQueueUseCaseTest {

    private lateinit var waitingQueueRepository: FakeWaitingQueueRepository
    private lateinit var entryTokenRepository: FakeEntryTokenRepository
    private lateinit var queueFallbackHandler: QueueFallbackHandler
    private lateinit var enterQueueUseCase: EnterQueueUseCase

    private val maxCapacity = 50_000
    private val throughputTps = 175

    @BeforeEach
    fun setUp() {
        waitingQueueRepository = FakeWaitingQueueRepository()
        entryTokenRepository = FakeEntryTokenRepository()
        queueFallbackHandler = QueueFallbackHandler()
        enterQueueUseCase = EnterQueueUseCase(
            waitingQueueRepository = waitingQueueRepository,
            entryTokenRepository = entryTokenRepository,
            queueFallbackHandler = queueFallbackHandler,
            queueProperties = QueueProperties(
                maxCapacity = maxCapacity,
                batchSize = 18,
                tokenTtlSeconds = 300,
                throughputTps = throughputTps,
                schedulerDelayMs = 100,
            ),
        )
    }

    @Nested
    @DisplayName("execute 시")
    inner class Execute {

        @Test
        @DisplayName("이미 토큰을 보유 중이면 토큰 정보를 반환한다")
        fun execute_existingToken_returnsTokenInfo() {
            // arrange
            entryTokenRepository.issue(UserId(1L), "existing-token", 300)

            // act
            val result = enterQueueUseCase.execute(1L)

            // assert
            assertThat(result.token).isEqualTo("existing-token")
            assertThat(result.position).isEqualTo(0)
            assertThat(result.estimatedWaitSeconds).isEqualTo(0)
        }

        @Test
        @DisplayName("이미 대기열에 있으면 기존 순번을 반환한다")
        fun execute_alreadyInQueue_returnsExistingPosition() {
            // arrange
            waitingQueueRepository.enter(UserId(1L), maxCapacity)
            waitingQueueRepository.enter(UserId(2L), maxCapacity)

            // act
            val result = enterQueueUseCase.execute(2L)

            // assert
            assertThat(result.position).isEqualTo(1L)
            assertThat(result.token).isNull()
        }

        @Test
        @DisplayName("대기열 상한 초과 시 TOO_MANY_REQUESTS 예외가 발생한다")
        fun execute_capacityExceeded_throwsTooManyRequests() {
            // arrange
            val smallCapacityProps = QueueProperties(
                maxCapacity = 2,
                batchSize = 18,
                tokenTtlSeconds = 300,
                throughputTps = throughputTps,
                schedulerDelayMs = 100,
            )
            val useCase = EnterQueueUseCase(
                waitingQueueRepository = waitingQueueRepository,
                entryTokenRepository = entryTokenRepository,
                queueFallbackHandler = queueFallbackHandler,
                queueProperties = smallCapacityProps,
            )
            waitingQueueRepository.enter(UserId(1L), 2)
            waitingQueueRepository.enter(UserId(2L), 2)

            // act
            val exception = assertThrows<CoreException> {
                useCase.execute(3L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.TOO_MANY_REQUESTS)
        }

        @Test
        @DisplayName("신규 진입 시 순번과 예상 대기 시간을 반환한다")
        fun execute_newEntry_returnsPositionAndEstimatedWait() {
            // arrange — 350명을 먼저 진입시켜 예상 대기 시간 검증 (350 / 175 = 2초)
            repeat(350) { i ->
                waitingQueueRepository.enter(UserId(i.toLong() + 100), maxCapacity)
            }

            // act
            val result = enterQueueUseCase.execute(1L)

            // assert
            assertThat(result.position).isEqualTo(350L)
            assertThat(result.estimatedWaitSeconds).isEqualTo(2L)
            assertThat(result.token).isNull()
        }

        @Test
        @DisplayName("fallback 상태에서 호출 시 SERVICE_UNAVAILABLE 예외가 발생한다")
        fun execute_fallbackActive_throwsServiceUnavailable() {
            // arrange
            queueFallbackHandler.markUnavailable("Redis 장애")

            // act
            val exception = assertThrows<CoreException> {
                enterQueueUseCase.execute(1L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.SERVICE_UNAVAILABLE)
        }
    }

    @Nested
    @DisplayName("Redis 장애 시")
    inner class RedisFailure {

        @Test
        @DisplayName("entryTokenRepository 호출 중 예외 발생 시 503 반환 + fallback 전환한다")
        fun execute_entryTokenRedisFailure_throwsServiceUnavailableAndMarkUnavailable() {
            // arrange
            val failingEntryTokenRepo = object : EntryTokenRepository {
                override fun find(userId: UserId): String? =
                    throw RuntimeException("Redis connection refused")

                override fun issue(userId: UserId, token: String, ttlSeconds: Long) = Unit
                override fun delete(userId: UserId) = Unit
                override fun consumeIfValid(userId: UserId, token: String) =
                    throw RuntimeException("Redis connection refused")
            }
            val useCase = EnterQueueUseCase(
                waitingQueueRepository = waitingQueueRepository,
                entryTokenRepository = failingEntryTokenRepo,
                queueFallbackHandler = queueFallbackHandler,
                queueProperties = QueueProperties(
                    maxCapacity = maxCapacity,
                    batchSize = 18,
                    tokenTtlSeconds = 300,
                    throughputTps = throughputTps,
                    schedulerDelayMs = 100,
                    ),
            )

            // act
            val exception = assertThrows<CoreException> {
                useCase.execute(1L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.SERVICE_UNAVAILABLE)
            assertThat(queueFallbackHandler.isAvailable()).isFalse()
        }

        @Test
        @DisplayName("waitingQueueRepository 호출 중 예외 발생 시 503 반환 + fallback 전환한다")
        fun execute_waitingQueueRedisFailure_throwsServiceUnavailableAndMarkUnavailable() {
            // arrange
            val failingWaitingQueueRepo = object : WaitingQueueRepository {
                override fun enter(userId: UserId, maxCapacity: Int): Long? =
                    throw RuntimeException("Redis connection refused")

                override fun findPosition(userId: UserId): Long? =
                    throw RuntimeException("Redis connection refused")

                override fun count(): Long =
                    throw RuntimeException("Redis connection refused")

                override fun popMin(count: Int): List<UserId> =
                    throw RuntimeException("Redis connection refused")
            }
            val useCase = EnterQueueUseCase(
                waitingQueueRepository = failingWaitingQueueRepo,
                entryTokenRepository = entryTokenRepository,
                queueFallbackHandler = queueFallbackHandler,
                queueProperties = QueueProperties(
                    maxCapacity = maxCapacity,
                    batchSize = 18,
                    tokenTtlSeconds = 300,
                    throughputTps = throughputTps,
                    schedulerDelayMs = 100,
                    ),
            )

            // act
            val exception = assertThrows<CoreException> {
                useCase.execute(1L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.SERVICE_UNAVAILABLE)
            assertThat(queueFallbackHandler.isAvailable()).isFalse()
        }
    }
}
