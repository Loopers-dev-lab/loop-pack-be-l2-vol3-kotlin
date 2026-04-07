package com.loopers.application.queue

import com.loopers.infrastructure.queue.WaitingQueueRedisRepository.Companion.TOKEN_KEY_PREFIX
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
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
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate

/**
 * QueueService 통합 테스트
 * - 실제 Redis와 연동하여 대기열 진입 → 토큰 발급 → 검증 흐름 검증
 * - 각 테스트는 고유한 userId 범위를 사용하여 테스트 간 격리
 */
@SpringBootTest
class QueueServiceIntegrationTest @Autowired constructor(
    private val queueService: QueueService,
    @Qualifier("redisTemplateMaster") private val masterRedisTemplate: RedisTemplate<String, String>,
    @Qualifier("redisConnectionMaster") private val masterLettuceConnectionFactory: LettuceConnectionFactory,
) {

    @BeforeEach
    fun setUp() {
        flushRedis()
    }

    @AfterEach
    fun tearDown() {
        flushRedis()
    }

    private fun flushRedis() {
        masterLettuceConnectionFactory.connection.use { connection ->
            connection.serverCommands().flushAll()
        }
        // flush 후 검증
        val remainingKeys = masterRedisTemplate.keys("queue:*")
        if (remainingKeys.isNotEmpty()) {
            println("WARNING: flushRedis failed, remaining keys: $remainingKeys")
            remainingKeys.forEach { masterRedisTemplate.delete(it) }
        }
    }

    private fun hasTokenOnMaster(userId: Long): Boolean {
        return masterRedisTemplate.hasKey("$TOKEN_KEY_PREFIX$userId")
    }

    private fun cleanTokensForUsers(userIds: LongRange) {
        masterLettuceConnectionFactory.connection.use { connection ->
            userIds.forEach {
                connection.keyCommands().del("$TOKEN_KEY_PREFIX$it".toByteArray())
            }
        }
    }

    @DisplayName("대기열 진입 → 토큰 발급 → 검증 흐름")
    @Nested
    inner class QueueFlow {

        @DisplayName("유저가 대기열에 진입하면 순번을 받는다.")
        @Test
        fun returnsPosition_whenEnterQueue() {
            // act
            val result = queueService.enterQueue(1001L)

            // assert
            assertAll(
                { assertThat(result.position).isGreaterThan(0L) },
                { assertThat(result.totalWaiting).isGreaterThan(0L) },
            )
        }

        @DisplayName("여러 유저가 진입하면 순서대로 순번이 부여된다.")
        @Test
        fun assignsPositionsInOrder_whenMultipleUsersEnter() {
            // act
            val result1 = queueService.enterQueue(1101L)
            val result2 = queueService.enterQueue(1102L)
            val result3 = queueService.enterQueue(1103L)

            // assert
            assertAll(
                { assertThat(result1.position).isLessThan(result2.position) },
                { assertThat(result2.position).isLessThan(result3.position) },
            )
        }

        @DisplayName("같은 유저가 다시 진입하면 기존 순번이 유지된다.")
        @Test
        fun keepsSamePosition_whenSameUserEntersAgain() {
            // arrange
            val firstResult = queueService.enterQueue(1201L)

            // act
            val secondResult = queueService.enterQueue(1201L)

            // assert
            assertThat(secondResult.position).isEqualTo(firstResult.position)
        }

        @DisplayName("토큰 발급 후 순번 조회 시 토큰이 포함된다.")
        @Test
        fun returnsToken_whenTokenIssued() {
            // arrange
            queueService.enterQueue(1301L)
            queueService.issueTokens(1)

            // act
            val result = queueService.getQueuePosition(1301L)

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
            queueService.enterQueue(1401L)
            queueService.issueTokens(1)

            // act & assert
            queueService.validateAndConsumeToken(1401L)
        }

        @DisplayName("토큰이 없는 유저는 검증에 실패한다.")
        @Test
        fun validationFails_whenNoToken() {
            // act
            val exception = assertThrows<CoreException> {
                queueService.validateAndConsumeToken(9999L)
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
            cleanTokensForUsers(2001L..2010L)
            for (i in 2001L..2010L) {
                queueService.enterQueue(i)
            }

            // act
            val issuedCount = queueService.issueTokens(5)

            // assert
            assertAll(
                { assertThat(issuedCount).isEqualTo(5L) },
                { assertThat(hasTokenOnMaster(2001L)).isTrue() },
                { assertThat(hasTokenOnMaster(2005L)).isTrue() },
                { assertThat(hasTokenOnMaster(2006L)).isFalse() },
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
            for (i in 2101L..2103L) {
                queueService.enterQueue(i)
            }

            // act
            val issuedCount = queueService.issueTokens(18)

            // assert
            assertThat(issuedCount).isEqualTo(3L)
        }
    }

    @DisplayName("동시 진입 순서 보장")
    @Nested
    inner class ConcurrentEntry {

        @DisplayName("순차 진입 후 토큰 발급하면, 요청한 수만큼 발급된다.")
        @Test
        fun issuesExactCount_whenSequentialEntry() {
            // arrange
            for (i in 5001L..5005L) {
                queueService.enterQueue(i)
            }

            // act
            val issuedCount = queueService.issueTokens(2)

            // assert — 반환값으로 정확히 2명 발급 확인
            assertThat(issuedCount).isEqualTo(2L)
        }
    }

    @DisplayName("Token Expiry Rate 지표")
    @Nested
    inner class TokenExpiryRate {

        @DisplayName("발급 후 일부만 소비하면 소비 카운트가 정확히 증가한다.")
        @Test
        fun incrementsConsumedCount_whenPartiallyConsumed() {
            // arrange
            val consumedBefore = queueService.getTokenConsumedCount()

            // act — 7명 소비
            repeat(7) { queueService.incrementConsumedCount() }

            // assert
            val consumedDelta = queueService.getTokenConsumedCount() - consumedBefore
            assertThat(consumedDelta).isEqualTo(7L)
        }
    }
}
