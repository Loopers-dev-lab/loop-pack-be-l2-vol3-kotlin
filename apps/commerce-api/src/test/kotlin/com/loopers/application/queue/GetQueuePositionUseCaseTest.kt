package com.loopers.application.queue

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.queue.token.FakeEntryTokenRepository
import com.loopers.domain.queue.token.repository.EntryTokenRepository
import com.loopers.domain.queue.waiting.FakeWaitingQueueRepository
import com.loopers.domain.queue.waiting.repository.WaitingQueueRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.springframework.dao.DataAccessResourceFailureException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GetQueuePositionUseCaseTest {

    private lateinit var waitingQueueRepository: FakeWaitingQueueRepository
    private lateinit var entryTokenRepository: FakeEntryTokenRepository
    private lateinit var queueFallbackHandler: QueueFallbackHandler
    private lateinit var getQueuePositionUseCase: GetQueuePositionUseCase

    private val maxCapacity = 50_000
    private val throughputTps = 180

    @BeforeEach
    fun setUp() {
        waitingQueueRepository = FakeWaitingQueueRepository()
        entryTokenRepository = FakeEntryTokenRepository()
        queueFallbackHandler = QueueFallbackHandler()
        getQueuePositionUseCase = GetQueuePositionUseCase(
            waitingQueueRepository = waitingQueueRepository,
            entryTokenRepository = entryTokenRepository,
            queueFallbackHandler = queueFallbackHandler,
            queueProperties = QueueProperties(
                maxCapacity = maxCapacity,
                batchSize = 18,
                tokenTtlSeconds = 300,
                schedulerDelayMs = 100,
            ),
        )
    }

    @Nested
    @DisplayName("execute 시")
    inner class Execute {

        @Test
        @DisplayName("대기열에 없는 유저 조회 시 NOT_FOUND 예외가 발생한다")
        fun execute_notInQueue_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                getQueuePositionUseCase.execute(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("토큰 보유 시 토큰 정보를 포함하여 반환한다")
        fun execute_withToken_returnsTokenInfo() {
            // arrange
            entryTokenRepository.issue(UserId(1L), "issued-token", 300)

            // act
            val result = getQueuePositionUseCase.execute(1L)

            // assert
            assertThat(result.token).isEqualTo("issued-token")
            assertThat(result.position).isEqualTo(0)
            assertThat(result.estimatedWaitSeconds).isEqualTo(0)
        }

        @Test
        @DisplayName("순번 50 이하이면 추천 폴링 주기 1000ms ±20% 범위를 반환한다")
        fun execute_position50OrLess_pollInterval1000WithJitter() {
            // arrange
            waitingQueueRepository.enter(UserId(1L), maxCapacity)
            waitingQueueRepository.enter(UserId(2L), maxCapacity)

            // act
            val result = getQueuePositionUseCase.execute(2L)

            // assert
            assertThat(result.position).isEqualTo(1L)
            assertThat(result.recommendedPollIntervalMs).isBetween(800L, 1200L)
        }

        @Test
        @DisplayName("토큰 보유 시 추천 폴링 주기 0ms를 반환한다")
        fun execute_withToken_pollInterval0() {
            // arrange
            entryTokenRepository.issue(UserId(1L), "token", 300)

            // act
            val result = getQueuePositionUseCase.execute(1L)

            // assert
            assertThat(result.position).isEqualTo(0L)
            assertThat(result.estimatedWaitSeconds).isEqualTo(0L)
            assertThat(result.recommendedPollIntervalMs).isEqualTo(0L)
        }

        @Test
        @DisplayName("정상 조회 시 순번과 예상 대기 시간을 반환한다")
        fun execute_inQueue_returnsPositionAndEstimatedWait() {
            // arrange
            waitingQueueRepository.enter(UserId(1L), maxCapacity)
            waitingQueueRepository.enter(UserId(2L), maxCapacity)
            waitingQueueRepository.enter(UserId(3L), maxCapacity)

            // act
            val result = getQueuePositionUseCase.execute(3L)

            // assert
            assertThat(result.position).isEqualTo(2L)
            assertThat(result.estimatedWaitSeconds).isEqualTo(2L / throughputTps)
            assertThat(result.token).isNull()
        }

        @Test
        @DisplayName("순번 360, throughputTps 180일 때 예상 대기 시간 2초를 반환한다")
        fun execute_position360_estimatedWait2Seconds() {
            // arrange — 361명 진입 (마지막 유저 position=360)
            for (i in 1L..361L) {
                waitingQueueRepository.enter(UserId(i), maxCapacity)
            }

            // act
            val result = getQueuePositionUseCase.execute(361L)

            // assert
            assertThat(result.position).isEqualTo(360L)
            assertThat(result.estimatedWaitSeconds).isEqualTo(2L)
        }

        @Test
        @DisplayName("순번 51일 때 추천 폴링 주기 2000ms ±20% 범위를 반환한다")
        fun execute_position51_pollInterval2000WithJitter() {
            // arrange — 52명 진입 (마지막 유저 position=51)
            for (i in 1L..52L) {
                waitingQueueRepository.enter(UserId(i), maxCapacity)
            }

            // act
            val result = getQueuePositionUseCase.execute(52L)

            // assert
            assertThat(result.position).isEqualTo(51L)
            assertThat(result.recommendedPollIntervalMs).isBetween(1600L, 2400L)
        }

        @Test
        @DisplayName("fallback 상태에서 호출 시 SERVICE_UNAVAILABLE 예외가 발생한다")
        fun execute_fallbackActive_throwsServiceUnavailable() {
            // arrange
            queueFallbackHandler.markUnavailable()

            // act
            val exception = assertThrows<CoreException> {
                getQueuePositionUseCase.execute(1L)
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
                    throw DataAccessResourceFailureException("Redis connection refused")

                override fun issue(userId: UserId, token: String, ttlSeconds: Long) = Unit
                override fun delete(userId: UserId) = Unit
                override fun consumeIfValid(userId: UserId, token: String) =
                    throw DataAccessResourceFailureException("Redis connection refused")
            }
            val useCase = GetQueuePositionUseCase(
                waitingQueueRepository = waitingQueueRepository,
                entryTokenRepository = failingEntryTokenRepo,
                queueFallbackHandler = queueFallbackHandler,
                queueProperties = QueueProperties(
                    maxCapacity = maxCapacity,
                    batchSize = 18,
                    tokenTtlSeconds = 300,
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
                    throw DataAccessResourceFailureException("Redis connection refused")

                override fun findPosition(userId: UserId): Long? =
                    throw DataAccessResourceFailureException("Redis connection refused")

                override fun count(): Long =
                    throw DataAccessResourceFailureException("Redis connection refused")

                override fun popMin(count: Int): List<UserId> =
                    throw DataAccessResourceFailureException("Redis connection refused")
            }
            val useCase = GetQueuePositionUseCase(
                waitingQueueRepository = failingWaitingQueueRepo,
                entryTokenRepository = entryTokenRepository,
                queueFallbackHandler = queueFallbackHandler,
                queueProperties = QueueProperties(
                    maxCapacity = maxCapacity,
                    batchSize = 18,
                    tokenTtlSeconds = 300,
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
