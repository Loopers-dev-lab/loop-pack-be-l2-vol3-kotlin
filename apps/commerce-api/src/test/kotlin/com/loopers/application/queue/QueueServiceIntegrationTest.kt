package com.loopers.application.queue

import com.loopers.infrastructure.queue.WaitingQueueRedisRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate

/**
 * QueueService 통합 테스트
 * - 실제 Redis(TestContainers)와 연동하여 대기열 진입 → 토큰 발급 → 검증 흐름 검증
 */
@SpringBootTest
class QueueServiceIntegrationTest @Autowired constructor(
    private val queueService: QueueService,
    private val waitingQueueRedisRepository: WaitingQueueRedisRepository,
    private val redisCleanUp: RedisCleanUp,
    @Qualifier("redisTemplateMaster") private val masterRedisTemplate: RedisTemplate<String, String>,
) {

    @BeforeEach
    fun setUp() {
        redisCleanUp.truncateAll()
    }

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @DisplayName("대기열 진입 → 토큰 발급 → 검증 흐름")
    @Nested
    inner class QueueFlow {

        @DisplayName("유저가 대기열에 진입하면 순번을 받는다.")
        @Test
        fun returnsPosition_whenEnterQueue() {
            // act
            val result = queueService.enterQueue(1L)

            // assert
            assertAll(
                { assertThat(result.position).isEqualTo(1L) },
                { assertThat(result.totalWaiting).isEqualTo(1L) },
            )
        }

        @DisplayName("여러 유저가 진입하면 순서대로 순번이 부여된다.")
        @Test
        fun assignsPositionsInOrder_whenMultipleUsersEnter() {
            // act
            val result1 = queueService.enterQueue(1L)
            val result2 = queueService.enterQueue(2L)
            val result3 = queueService.enterQueue(3L)

            // assert
            assertAll(
                { assertThat(result1.position).isEqualTo(1L) },
                { assertThat(result2.position).isEqualTo(2L) },
                { assertThat(result3.position).isEqualTo(3L) },
                { assertThat(result3.totalWaiting).isEqualTo(3L) },
            )
        }

        @DisplayName("같은 유저가 다시 진입하면 기존 순번이 유지된다.")
        @Test
        fun keepsSamePosition_whenSameUserEntersAgain() {
            // arrange
            queueService.enterQueue(1L)
            queueService.enterQueue(2L)

            // act
            val result = queueService.enterQueue(1L)

            // assert
            assertThat(result.position).isEqualTo(1L)
        }

        @DisplayName("토큰 발급 후 순번 조회 시 토큰이 포함된다.")
        @Test
        fun returnsToken_whenTokenIssued() {
            // arrange
            queueService.enterQueue(1L)
            queueService.issueTokens(1)

            // act
            val result = queueService.getQueuePosition(1L)

            // assert
            assertAll(
                { assertThat(result.position).isEqualTo(0L) },
                { assertThat(result.token).isNotNull() },
            )
        }

        @DisplayName("토큰이 발급된 유저는 검증에 성공한다.")
        @Test
        fun validationSucceeds_whenTokenExists() {
            // arrange
            queueService.enterQueue(1L)
            queueService.issueTokens(1)

            // act & assert
            queueService.validateAndConsumeToken(1L)
        }

        @DisplayName("토큰이 없는 유저는 검증에 실패한다.")
        @Test
        fun validationFails_whenNoToken() {
            // act
            val exception = assertThrows<CoreException> {
                queueService.validateAndConsumeToken(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.FORBIDDEN)
        }
    }

    @DisplayName("토큰 발급 배치 처리")
    @Nested
    inner class TokenIssueBatch {

        @DisplayName("배치 크기만큼 토큰을 발급한다.")
        @Test
        fun issuesTokensInBatch_whenMultipleUsersInQueue() {
            // arrange
            for (i in 1L..10L) {
                queueService.enterQueue(i)
            }

            // act
            val issuedCount = queueService.issueTokens(5)

            // assert
            assertAll(
                { assertThat(issuedCount).isEqualTo(5L) },
                { assertThat(waitingQueueRedisRepository.getTotalCount()).isEqualTo(5L) },
                { assertThat(waitingQueueRedisRepository.hasToken(1L)).isTrue() },
                { assertThat(waitingQueueRedisRepository.hasToken(5L)).isTrue() },
                { assertThat(waitingQueueRedisRepository.hasToken(6L)).isFalse() },
            )
        }

        @DisplayName("대기열이 비어있으면 0건을 발급한다.")
        @Test
        fun issuesZero_whenQueueEmpty() {
            // act
            val issuedCount = queueService.issueTokens(18)

            // assert
            assertThat(issuedCount).isEqualTo(0L)
        }

        @DisplayName("대기열보다 배치 크기가 크면 대기열 전체만 발급한다.")
        @Test
        fun issuesOnlyAvailable_whenBatchSizeLargerThanQueue() {
            // arrange
            for (i in 1L..3L) {
                queueService.enterQueue(i)
            }

            // act
            val issuedCount = queueService.issueTokens(18)

            // assert
            assertAll(
                { assertThat(issuedCount).isEqualTo(3L) },
                { assertThat(waitingQueueRedisRepository.getTotalCount()).isEqualTo(0L) },
            )
        }
    }

    @DisplayName("동시 진입 순서 보장")
    @Nested
    inner class ConcurrentEntry {

        @DisplayName("순차 진입 시 먼저 진입한 유저가 먼저 토큰을 받는다.")
        @Test
        fun firstInFirstOut_whenSequentialEntry() {
            // arrange
            for (i in 1L..5L) {
                queueService.enterQueue(i)
            }

            // act — 2명에게만 토큰 발급
            queueService.issueTokens(2)

            // assert — userId 1, 2만 토큰 보유
            assertAll(
                { assertThat(waitingQueueRedisRepository.hasToken(1L)).isTrue() },
                { assertThat(waitingQueueRedisRepository.hasToken(2L)).isTrue() },
                { assertThat(waitingQueueRedisRepository.hasToken(3L)).isFalse() },
            )
        }
    }

    @DisplayName("Token Expiry Rate 지표")
    @Nested
    inner class TokenExpiryRate {

        @DisplayName("발급 후 일부만 소비하면 만료율이 정확히 계산된다.")
        @Test
        fun calculatesExpiryRate_whenPartiallyConsumed() {
            // arrange — 기존 카운터 기록
            val issuedBefore = queueService.getTokenIssuedCount()
            val consumedBefore = queueService.getTokenConsumedCount()

            // 10명 진입 후 토큰 발급
            for (i in 1L..10L) {
                queueService.enterQueue(i)
            }
            queueService.issueTokens(10)

            // act — 7명만 소비
            repeat(7) { queueService.incrementConsumedCount() }

            // assert — 상대값으로 검증
            val issuedDelta = queueService.getTokenIssuedCount() - issuedBefore
            val consumedDelta = queueService.getTokenConsumedCount() - consumedBefore
            assertAll(
                { assertThat(issuedDelta).isEqualTo(10L) },
                { assertThat(consumedDelta).isEqualTo(7L) },
            )
        }
    }
}
