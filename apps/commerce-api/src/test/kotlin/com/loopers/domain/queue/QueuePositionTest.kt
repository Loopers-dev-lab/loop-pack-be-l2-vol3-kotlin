package com.loopers.domain.queue

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class QueuePositionTest {

    @Nested
    @DisplayName("QueuePosition 생성할 때,")
    inner class QueuePositionCreation {

        @Test
        @DisplayName("대기 순번, 예상 대기 시간, 전체 대기 인원, 토큰을 가진다")
        fun `대기 순번, 예상 대기 시간, 전체 대기 인원, 토큰을 가진다`() {
            // arrange
            val position = 10L
            val estimatedWaitSeconds = 5.0
            val totalSize = 100L
            val token = "test-token"

            // act
            val queuePosition = QueuePosition(
                position = position,
                estimatedWaitSeconds = estimatedWaitSeconds,
                totalSize = totalSize,
                token = token,
            )

            // assert
            assertAll(
                { assertThat(queuePosition.position).isEqualTo(10L) },
                { assertThat(queuePosition.estimatedWaitSeconds).isEqualTo(5.0) },
                { assertThat(queuePosition.totalSize).isEqualTo(100L) },
                { assertThat(queuePosition.token).isEqualTo("test-token") },
            )
        }

        @Test
        @DisplayName("토큰이 없으면 null이다")
        fun `토큰이 없으면 null이다`() {
            // arrange & act
            val queuePosition = QueuePosition(
                position = 5L,
                estimatedWaitSeconds = 2.5,
                totalSize = 50L,
            )

            // assert
            assertThat(queuePosition.token).isNull()
        }
    }
}
