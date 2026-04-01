package com.loopers.application.queue

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class QueuePositionInfoTest {

    @Nested
    @DisplayName("recommendedPollIntervalMs 계산 시")
    inner class RecommendedPollIntervalMs {

        @Test
        @DisplayName("token이 존재하면 0ms를 반환한다")
        fun withToken_returns0() {
            // act
            val info = QueuePositionInfo(position = 0, estimatedWaitSeconds = 0, token = "some-token")

            // assert
            assertThat(info.recommendedPollIntervalMs).isEqualTo(0L)
        }

        @Test
        @DisplayName("token이 null이고 position이 100 이하이면 1000ms를 반환한다")
        fun noToken_position100OrLess_returns1000() {
            // act
            val info = QueuePositionInfo(position = 100, estimatedWaitSeconds = 0, token = null)

            // assert
            assertThat(info.recommendedPollIntervalMs).isEqualTo(1000L)
        }

        @Test
        @DisplayName("token이 null이고 position이 101~1000이면 3000ms를 반환한다")
        fun noToken_position101To1000_returns3000() {
            // act
            val info = QueuePositionInfo(position = 500, estimatedWaitSeconds = 0, token = null)

            // assert
            assertThat(info.recommendedPollIntervalMs).isEqualTo(3000L)
        }

        @Test
        @DisplayName("token이 null이고 position이 1000 초과이면 5000ms를 반환한다")
        fun noToken_positionOver1000_returns5000() {
            // act
            val info = QueuePositionInfo(position = 1001, estimatedWaitSeconds = 0, token = null)

            // assert
            assertThat(info.recommendedPollIntervalMs).isEqualTo(5000L)
        }

        @Test
        @DisplayName("calculatePollIntervalMs에 token을 직접 전달하면 0ms를 반환한다")
        fun calculatePollIntervalMs_withToken_returns0() {
            // act
            val result = QueuePositionInfo.calculatePollIntervalMs(position = 500, token = "token")

            // assert
            assertThat(result).isEqualTo(0L)
        }
    }
}
