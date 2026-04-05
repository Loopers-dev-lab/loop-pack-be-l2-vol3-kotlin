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
            every { queueRepository.getRank(queueName, userId) } returns 9L // 10번째

            // act
            val result = queueService.enter(queueName, userId, throughput)

            // assert
            assertThat(result.position).isEqualTo(10L)
            assertThat(result.estimatedWaitSeconds).isEqualTo(0L) // 10 / 175 = 0 (정수 나눗셈)
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
