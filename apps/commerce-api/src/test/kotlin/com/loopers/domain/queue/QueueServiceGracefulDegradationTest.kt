package com.loopers.domain.queue

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class QueueServiceGracefulDegradationTest {

    private lateinit var queueService: QueueService
    private lateinit var queueRepository: QueueRepository

    @BeforeEach
    fun setUp() {
        queueRepository = mockk()
        queueService = QueueService(queueRepository)
    }

    @DisplayName("enter 메서드 - Redis 장애 시 503 변환")
    @Nested
    inner class EnterRedisFailureGracefulDegradationTest {

        @Test
        fun `Redis atomicUpsert 실패 시 SERVICE_TEMPORARILY_UNAVAILABLE 예외 발생`() {
            // arrange
            val queueName = "order-queue"
            val userId = 100L
            val throughput = 175
            every { queueRepository.atomicUpsertWithSequence(queueName, userId) } throws RuntimeException("Redis 연결 실패")

            // act & assert
            val exception = org.junit.jupiter.api.assertThrows<CoreException> {
                queueService.enter(queueName, userId, throughput)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.SERVICE_TEMPORARILY_UNAVAILABLE)
        }

        @Test
        fun `Redis getRank 실패 시 SERVICE_TEMPORARILY_UNAVAILABLE 예외 발생`() {
            // arrange
            val queueName = "order-queue"
            val userId = 100L
            val throughput = 175
            every { queueRepository.atomicUpsertWithSequence(queueName, userId) } returns 1.0
            every { queueRepository.getToken(queueName, userId) } returns null
            every { queueRepository.getRank(queueName, userId) } throws RuntimeException("Redis 연결 실패")

            // act & assert
            val exception = org.junit.jupiter.api.assertThrows<CoreException> {
                queueService.enter(queueName, userId, throughput)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.SERVICE_TEMPORARILY_UNAVAILABLE)
        }

        @Test
        fun `CoreException은 그대로 전파된다`() {
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
    }
}
