package com.loopers.domain.queue

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class QueueServiceTest {

    private lateinit var queueService: QueueService
    private lateinit var queueRepository: QueueRepository

    @BeforeEach
    fun setUp() {
        queueRepository = mockk()
        queueService = QueueService(queueRepository)
    }

    @DisplayName("enter 메서드 테스트")
    @Nested
    inner class EnterTest {

        @Test
        fun `정상 진입시 순번과 예상 대기시간을 반환한다`() {
            // arrange
            val queueName = "order-queue"
            val userId = 100L
            val throughput = 175
            every { queueRepository.atomicUpsertWithSequence(queueName, userId) } returns 1.0
            every { queueRepository.getToken(queueName, userId) } returns null
            every { queueRepository.getRank(queueName, userId) } returns 0L

            // act
            val result = queueService.enter(queueName, userId, throughput)

            // assert
            assertThat(result).isNotNull()
            assertThat(result.queueName).isEqualTo(queueName)
            assertThat(result.userId).isEqualTo(userId)
            assertThat(result.position).isEqualTo(1L)
            assertThat(result.estimatedWaitSeconds).isEqualTo(0L)
            verify { queueRepository.atomicUpsertWithSequence(queueName, userId) }
        }

        @Test
        fun `이미 대기열에 있는 userId로 재진입시 새 시퀀스로 upsert되어 맨 뒤로 이동한다`() {
            // arrange: atomic upsert는 기존 항목을 제거하고 새 시퀀스로 재삽입
            val queueName = "order-queue"
            val userId = 100L
            val throughput = 175
            every { queueRepository.atomicUpsertWithSequence(queueName, userId) } returns 10.0
            every { queueRepository.getToken(queueName, userId) } returns null
            every { queueRepository.getRank(queueName, userId) } returns 9L // 맨 뒤 (10번째)

            // act
            val result = queueService.enter(queueName, userId, throughput)

            // assert
            assertThat(result.position).isEqualTo(10L)
            verify { queueRepository.atomicUpsertWithSequence(queueName, userId) }
        }

        @Test
        fun `진입 후 getRank가 null이면 QUEUE_NOT_FOUND 예외가 발생한다`() {
            // arrange
            val queueName = "order-queue"
            val userId = 100L
            val throughput = 175
            every { queueRepository.atomicUpsertWithSequence(queueName, userId) } returns 1.0
            every { queueRepository.getToken(queueName, userId) } returns null
            every { queueRepository.getRank(queueName, userId) } returns null

            // act & assert
            val exception = org.junit.jupiter.api.assertThrows<CoreException> {
                queueService.enter(queueName, userId, throughput)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.QUEUE_NOT_FOUND)
        }

        @Test
        fun `10명이 진입했을 때 처리량 175로 예상 대기시간이 올바르게 계산된다`() {
            // arrange
            val queueName = "order-queue"
            val userId = 100L
            val throughput = 175
            every { queueRepository.atomicUpsertWithSequence(queueName, userId) } returns 10.0
            every { queueRepository.getToken(queueName, userId) } returns null
            every { queueRepository.getRank(queueName, userId) } returns 9L // 10번째

            // act
            val result = queueService.enter(queueName, userId, throughput)

            // assert
            assertThat(result.position).isEqualTo(10L)
            assertThat(result.estimatedWaitSeconds).isEqualTo(0L) // 10 / 175 = 0 (정수 나눗셈)
        }

        @Test
        @DisplayName("Transient 상태: token이 발급되었지만 rank가 아직 null인 경우 position 0을 반환한다")
        fun `transient_state_token_issued_before_rank_visible`() {
            // arrange: popMin 후 issueToken 사이의 transient 상태를 시뮬레이션
            val queueName = "order-queue"
            val userId = 100L
            val throughput = 175
            val issuedToken = "token-abc-123"

            every { queueRepository.atomicUpsertWithSequence(queueName, userId) } returns 1.0
            // getToken: 토큰이 이미 발급됨
            every { queueRepository.getToken(queueName, userId) } returns issuedToken
            // getRank: 아직 호출되지 않음 (getToken이 먼저 true를 반환하므로)

            // act
            val result = queueService.enter(queueName, userId, throughput)

            // assert: 토큰이 발급되었으므로 position 0 반환
            assertThat(result.position).isEqualTo(0L)
            assertThat(result.estimatedWaitSeconds).isEqualTo(0L)
            // getRank를 호출하지 않음 (getToken이 null이 아니므로)
            verify(exactly = 0) { queueRepository.getRank(queueName, userId) }
        }
    }

    @DisplayName("getPosition 메서드 테스트")
    @Nested
    inner class GetPositionTest {

        @Test
        fun `대기열에 있는 userId의 순번과 예상 대기시간을 반환한다`() {
            // arrange
            val queueName = "order-queue"
            val userId = 100L
            val throughput = 175
            every { queueRepository.getToken(queueName, userId) } returns null
            every { queueRepository.getRank(queueName, userId) } returns 5L

            // act
            val result = queueService.getPosition(queueName, userId, throughput)

            // assert
            assertThat(result.queueName).isEqualTo(queueName)
            assertThat(result.userId).isEqualTo(userId)
            assertThat(result.position).isEqualTo(6L)
            assertThat(result.estimatedWaitSeconds).isEqualTo(0L)
            assertThat(result.token).isNull()
        }

        @Test
        fun `토큰이 발급된 userId는 position 0과 token을 반환한다`() {
            // arrange
            val queueName = "order-queue"
            val userId = 100L
            val throughput = 175
            val expectedToken = "abc-123-def"
            every { queueRepository.getToken(queueName, userId) } returns expectedToken

            // act
            val result = queueService.getPosition(queueName, userId, throughput)

            // assert
            assertThat(result.queueName).isEqualTo(queueName)
            assertThat(result.userId).isEqualTo(userId)
            assertThat(result.position).isEqualTo(0L)
            assertThat(result.estimatedWaitSeconds).isEqualTo(0L)
            assertThat(result.token).isEqualTo(expectedToken)
        }

        @Test
        fun `대기열에 없고 토큰도 없는 userId는 QUEUE_NOT_FOUND 예외가 발생한다`() {
            // arrange
            val queueName = "order-queue"
            val userId = 100L
            val throughput = 175
            every { queueRepository.getToken(queueName, userId) } returns null
            every { queueRepository.getRank(queueName, userId) } returns null

            // act & assert
            val exception = org.junit.jupiter.api.assertThrows<CoreException> {
                queueService.getPosition(queueName, userId, throughput)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.QUEUE_NOT_FOUND)
        }

        @Test
        @DisplayName("Transient 상태: rank가 일시적으로 null이었다가 retry에서 발견되는 경우")
        fun `transient_state_rank_null_then_available_on_retry`() {
            // arrange: popMin 후 issueToken 사이의 transient 상태를 시뮬레이션
            // 첫 번째 getRank 호출: null (transient)
            // 두 번째 getRank 호출: rank 값 반환 (retry 성공)
            val queueName = "order-queue"
            val userId = 100L
            val throughput = 175

            every { queueRepository.getToken(queueName, userId) } returns null
            every { queueRepository.getRank(queueName, userId) }
                .returnsMany(null, 5L) // 첫 번째 호출: null, 두 번째 호출: 5L

            // act
            val result = queueService.getPosition(queueName, userId, throughput)

            // assert: retry를 통해 rank를 얻고 position 계산
            assertThat(result.position).isEqualTo(6L)
            assertThat(result.estimatedWaitSeconds).isEqualTo(0L)
            assertThat(result.token).isNull()
            // getRank가 2회 호출됨 (첫 번째 실패 + retry 성공)
            verify(atLeast = 2) { queueRepository.getRank(queueName, userId) }
        }
    }

    @DisplayName("getStatus 메서드 테스트")
    @Nested
    inner class GetStatusTest {

        @Test
        fun `대기열 이름과 전체 크기, 처리량을 반환한다`() {
            // arrange
            val queueName = "order-queue"
            val throughput = 175
            every { queueRepository.size(queueName) } returns 512L

            // act
            val result = queueService.getStatus(queueName, throughput)

            // assert
            assertThat(result.queueName).isEqualTo(queueName)
            assertThat(result.totalWaiting).isEqualTo(512L)
            assertThat(result.throughputPerSecond).isEqualTo(175L)
        }

        @Test
        fun `처리량이 그대로 throughputPerSecond로 반환된다`() {
            // arrange
            val queueName = "coupon-queue"
            val throughput = 500
            every { queueRepository.size(queueName) } returns 100L

            // act
            val result = queueService.getStatus(queueName, throughput)

            // assert
            assertThat(result.throughputPerSecond).isEqualTo(500L)
        }
    }
}
