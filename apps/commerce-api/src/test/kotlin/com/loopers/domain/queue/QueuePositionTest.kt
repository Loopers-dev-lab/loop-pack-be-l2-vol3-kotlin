package com.loopers.domain.queue

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("QueuePosition 테스트")
class QueuePositionTest {

    companion object {
        private const val BATCH_SIZE = 300
        private const val SCHEDULER_INTERVAL_SECONDS = 3
    }

    @Nested
    @DisplayName("estimatedWaitSeconds")
    inner class EstimatedWaitSeconds {

        @Test
        @DisplayName("순번이 배치 크기보다 작으면 스케줄러 1회 대기")
        fun `순번이 배치 크기 이내`() {
            val position = QueuePosition(
                position = 100,
                totalWaiting = 500,
                batchSize = BATCH_SIZE,
                schedulerIntervalSeconds = SCHEDULER_INTERVAL_SECONDS,
            )

            assertThat(position.estimatedWaitSeconds).isEqualTo(3)
        }

        @Test
        @DisplayName("순번이 배치 크기의 2배면 스케줄러 2회 대기")
        fun `순번이 배치 크기의 2배`() {
            val position = QueuePosition(
                position = 600,
                totalWaiting = 1000,
                batchSize = BATCH_SIZE,
                schedulerIntervalSeconds = SCHEDULER_INTERVAL_SECONDS,
            )

            assertThat(position.estimatedWaitSeconds).isEqualTo(6)
        }

        @Test
        @DisplayName("순번이 0이면 대기 시간 0")
        fun `순번이 0`() {
            val position = QueuePosition(
                position = 0,
                totalWaiting = 500,
                batchSize = BATCH_SIZE,
                schedulerIntervalSeconds = SCHEDULER_INTERVAL_SECONDS,
            )

            assertThat(position.estimatedWaitSeconds).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("retryAfter")
    inner class RetryAfter {

        @Test
        @DisplayName("순번 1~100이면 retryAfter 2초")
        fun `순번 100 이내`() {
            val position = QueuePosition(
                position = 50,
                totalWaiting = 500,
                batchSize = BATCH_SIZE,
                schedulerIntervalSeconds = SCHEDULER_INTERVAL_SECONDS,
            )

            assertThat(position.retryAfter).isEqualTo(2)
        }

        @Test
        @DisplayName("순번 101~500이면 retryAfter 5초")
        fun `순번 101에서 500`() {
            val position = QueuePosition(
                position = 300,
                totalWaiting = 500,
                batchSize = BATCH_SIZE,
                schedulerIntervalSeconds = SCHEDULER_INTERVAL_SECONDS,
            )

            assertThat(position.retryAfter).isEqualTo(5)
        }

        @Test
        @DisplayName("순번 501 이상이면 retryAfter 10초")
        fun `순번 501 이상`() {
            val position = QueuePosition(
                position = 1000,
                totalWaiting = 5000,
                batchSize = BATCH_SIZE,
                schedulerIntervalSeconds = SCHEDULER_INTERVAL_SECONDS,
            )

            assertThat(position.retryAfter).isEqualTo(10)
        }

        @Test
        @DisplayName("순번 0이면 retryAfter 0초 (토큰 발급됨)")
        fun `순번 0`() {
            val position = QueuePosition(
                position = 0,
                totalWaiting = 500,
                batchSize = BATCH_SIZE,
                schedulerIntervalSeconds = SCHEDULER_INTERVAL_SECONDS,
            )

            assertThat(position.retryAfter).isEqualTo(0)
        }

        @Test
        @DisplayName("순번 경계값 100은 retryAfter 2초")
        fun `경계값 100`() {
            val position = QueuePosition(
                position = 100,
                totalWaiting = 500,
                batchSize = BATCH_SIZE,
                schedulerIntervalSeconds = SCHEDULER_INTERVAL_SECONDS,
            )

            assertThat(position.retryAfter).isEqualTo(2)
        }

        @Test
        @DisplayName("순번 경계값 101은 retryAfter 5초")
        fun `경계값 101`() {
            val position = QueuePosition(
                position = 101,
                totalWaiting = 500,
                batchSize = BATCH_SIZE,
                schedulerIntervalSeconds = SCHEDULER_INTERVAL_SECONDS,
            )

            assertThat(position.retryAfter).isEqualTo(5)
        }

        @Test
        @DisplayName("순번 경계값 500은 retryAfter 5초")
        fun `경계값 500`() {
            val position = QueuePosition(
                position = 500,
                totalWaiting = 1000,
                batchSize = BATCH_SIZE,
                schedulerIntervalSeconds = SCHEDULER_INTERVAL_SECONDS,
            )

            assertThat(position.retryAfter).isEqualTo(5)
        }

        @Test
        @DisplayName("순번 경계값 501은 retryAfter 10초")
        fun `경계값 501`() {
            val position = QueuePosition(
                position = 501,
                totalWaiting = 1000,
                batchSize = BATCH_SIZE,
                schedulerIntervalSeconds = SCHEDULER_INTERVAL_SECONDS,
            )

            assertThat(position.retryAfter).isEqualTo(10)
        }
    }
}
