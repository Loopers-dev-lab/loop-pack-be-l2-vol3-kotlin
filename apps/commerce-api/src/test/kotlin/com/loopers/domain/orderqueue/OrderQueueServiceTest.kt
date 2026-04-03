package com.loopers.domain.orderqueue

import com.loopers.infrastructure.orderqueue.OrderQueueProperties
import com.loopers.domain.orderqueue.OrderQueueRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class OrderQueueServiceTest {
    private val orderQueueRepository = mockk<OrderQueueRepository>()
    private val orderQueueProperties = OrderQueueProperties()
    private val orderQueueService = OrderQueueService(orderQueueRepository, orderQueueProperties)

    companion object {
        private const val USER_ID = 1L
    }

    @DisplayName("enter")
    @Nested
    inner class Enter {
        @DisplayName("대기열 진입에 성공하면 QueueEntryInfo를 반환한다.")
        @Test
        fun returnsQueueEntryInfoOnSuccess() {
            // arrange
            val expectedPosition = 5L
            val expectedTotalWaiting = 10L
            every { orderQueueRepository.enqueue(USER_ID) } returns 1L
            every { orderQueueRepository.getPosition(USER_ID) } returns expectedPosition
            every { orderQueueRepository.getTotalSize() } returns expectedTotalWaiting

            // act
            val result = orderQueueService.enter(USER_ID)

            // assert
            assertAll(
                { assertThat(result.position).isEqualTo(expectedPosition) },
                { assertThat(result.totalWaiting).isEqualTo(expectedTotalWaiting) },
                { assertThat(result.estimatedWaitSeconds).isGreaterThan(0L) },
                { assertThat(result.pollingIntervalSeconds).isGreaterThan(0) },
            )
        }

        @DisplayName("중복 진입 시 CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflictOnDuplicate() {
            // arrange
            every { orderQueueRepository.enqueue(USER_ID) } returns 0L

            // act & assert
            val exception = assertThrows<CoreException> {
                orderQueueService.enter(USER_ID)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
        }

        @DisplayName("enqueue 성공 후 getPosition이 null이면 INTERNAL_ERROR 예외가 발생한다.")
        @Test
        fun throwsInternalErrorWhenPositionIsNullAfterEnqueue() {
            // arrange
            every { orderQueueRepository.enqueue(USER_ID) } returns 1L
            every { orderQueueRepository.getPosition(USER_ID) } returns null

            // act & assert
            val exception = assertThrows<CoreException> {
                orderQueueService.enter(USER_ID)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.INTERNAL_ERROR)
        }
    }

    @DisplayName("getPosition")
    @Nested
    inner class GetPosition {
        @DisplayName("대기열에 있으면 WAITING 상태와 순번을 반환한다.")
        @Test
        fun returnsWaitingStatusWhenInQueue() {
            // arrange
            val expectedPosition = 3L
            val expectedTotalWaiting = 10L
            every { orderQueueRepository.getTokenTtl(USER_ID) } returns -2L
            every { orderQueueRepository.getPosition(USER_ID) } returns expectedPosition
            every { orderQueueRepository.getTotalSize() } returns expectedTotalWaiting

            // act
            val result = orderQueueService.getPosition(USER_ID)

            // assert
            assertAll(
                { assertThat(result.status).isEqualTo(QueueStatus.WAITING) },
                { assertThat(result.position).isEqualTo(expectedPosition) },
                { assertThat(result.totalWaiting).isEqualTo(expectedTotalWaiting) },
                { assertThat(result.estimatedWaitSeconds).isNotNull() },
                { assertThat(result.pollingIntervalSeconds).isNotNull() },
                { assertThat(result.tokenExpireSeconds).isNull() },
            )
        }

        @DisplayName("토큰이 발급된 상태면 ACTIVE 상태를 반환한다.")
        @Test
        fun returnsActiveStatusWhenTokenIssued() {
            // arrange
            val expectedTtl = 250L
            every { orderQueueRepository.getTokenTtl(USER_ID) } returns expectedTtl

            // act
            val result = orderQueueService.getPosition(USER_ID)

            // assert
            assertAll(
                { assertThat(result.status).isEqualTo(QueueStatus.ACTIVE) },
                { assertThat(result.position).isNull() },
                { assertThat(result.totalWaiting).isNull() },
                { assertThat(result.tokenExpireSeconds).isEqualTo(expectedTtl) },
            )
        }

        @DisplayName("대기열에도 없고 토큰도 없으면 NOT_IN_QUEUE 상태를 반환한다.")
        @Test
        fun returnsNotInQueueWhenNeitherInQueueNorToken() {
            // arrange
            every { orderQueueRepository.getTokenTtl(USER_ID) } returns -2L
            every { orderQueueRepository.getPosition(USER_ID) } returns null

            // act
            val result = orderQueueService.getPosition(USER_ID)

            // assert
            assertAll(
                { assertThat(result.status).isEqualTo(QueueStatus.NOT_IN_QUEUE) },
                { assertThat(result.position).isNull() },
                { assertThat(result.totalWaiting).isNull() },
                { assertThat(result.tokenExpireSeconds).isNull() },
            )
        }
    }

    @DisplayName("consumeTokenOrThrow")
    @Nested
    inner class ConsumeTokenOrThrow {
        @DisplayName("토큰이 있으면 소비하고 정상 리턴한다.")
        @Test
        fun consumesTokenWhenPresent() {
            // arrange
            every { orderQueueRepository.consumeToken(USER_ID) } returns true

            // act & assert (예외 없으면 성공)
            orderQueueService.consumeTokenOrThrow(USER_ID)

            // verify
            verify(exactly = 1) { orderQueueRepository.consumeToken(USER_ID) }
        }

        @DisplayName("토큰이 없으면 BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestWhenNoToken() {
            // arrange
            every { orderQueueRepository.consumeToken(USER_ID) } returns false

            // act & assert
            val exception = assertThrows<CoreException> {
                orderQueueService.consumeTokenOrThrow(USER_ID)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("processTokenIssuance")
    @Nested
    inner class ProcessTokenIssuance {
        @DisplayName("dequeueAndIssueTokens를 원자적으로 호출한다.")
        @Test
        fun callsDequeueAndIssueTokensAtomically() {
            // arrange
            val batchSize = 7L
            every { orderQueueRepository.dequeueAndIssueTokens(batchSize, 300L) } returns 7L

            // act
            orderQueueService.processTokenIssuance(batchSize)

            // verify
            verify(exactly = 1) { orderQueueRepository.dequeueAndIssueTokens(batchSize, 300L) }
        }
    }

    @DisplayName("calculateEstimatedWaitSeconds")
    @Nested
    inner class CalculateEstimatedWaitSeconds {
        @DisplayName("순번 기반 예상 대기시간을 올림 나눗셈으로 계산한다.")
        @Test
        fun calculatesWithCeilDivision() {
            // 70 TPS 기준
            assertAll(
                { assertThat(orderQueueService.calculateEstimatedWaitSeconds(1L)).isEqualTo(1L) },
                { assertThat(orderQueueService.calculateEstimatedWaitSeconds(70L)).isEqualTo(1L) },
                { assertThat(orderQueueService.calculateEstimatedWaitSeconds(71L)).isEqualTo(2L) },
                { assertThat(orderQueueService.calculateEstimatedWaitSeconds(140L)).isEqualTo(2L) },
                { assertThat(orderQueueService.calculateEstimatedWaitSeconds(141L)).isEqualTo(3L) },
            )
        }
    }

    @DisplayName("calculatePollingInterval")
    @Nested
    inner class CalculatePollingInterval {
        @DisplayName("순번 1~100이면 2초를 반환한다.")
        @Test
        fun returnsTwoSecondsForPosition1To100() {
            assertAll(
                { assertThat(orderQueueService.calculatePollingInterval(1L)).isEqualTo(2) },
                { assertThat(orderQueueService.calculatePollingInterval(100L)).isEqualTo(2) },
            )
        }

        @DisplayName("순번 101~1000이면 5초를 반환한다.")
        @Test
        fun returnsFiveSecondsForPosition101To1000() {
            assertAll(
                { assertThat(orderQueueService.calculatePollingInterval(101L)).isEqualTo(5) },
                { assertThat(orderQueueService.calculatePollingInterval(1000L)).isEqualTo(5) },
            )
        }

        @DisplayName("순번 1001~5000이면 10초를 반환한다.")
        @Test
        fun returnsTenSecondsForPosition1001To5000() {
            assertAll(
                { assertThat(orderQueueService.calculatePollingInterval(1001L)).isEqualTo(10) },
                { assertThat(orderQueueService.calculatePollingInterval(5000L)).isEqualTo(10) },
            )
        }

        @DisplayName("순번 5001 이상이면 30초를 반환한다.")
        @Test
        fun returnsThirtySecondsForPosition5001AndAbove() {
            assertThat(orderQueueService.calculatePollingInterval(5001L)).isEqualTo(30)
        }
    }
}
