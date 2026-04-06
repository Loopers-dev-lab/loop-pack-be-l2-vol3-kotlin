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
        @DisplayName("token이 null이고 position이 50 이하이면 1000ms ±20% 범위를 반환한다")
        fun noToken_position50OrLess_returns1000WithJitter() {
            // act
            val info = QueuePositionInfo(position = 50, estimatedWaitSeconds = 0, token = null)

            // assert
            assertThat(info.recommendedPollIntervalMs).isBetween(800L, 1200L)
        }

        @Test
        @DisplayName("token이 null이고 position이 51~500이면 2000ms ±20% 범위를 반환한다")
        fun noToken_position51To500_returns2000WithJitter() {
            // act
            val info = QueuePositionInfo(position = 200, estimatedWaitSeconds = 0, token = null)

            // assert
            assertThat(info.recommendedPollIntervalMs).isBetween(1600L, 2400L)
        }

        @Test
        @DisplayName("token이 null이고 position이 500 초과이면 3000ms ±20% 범위를 반환한다")
        fun noToken_positionOver500_returns3000WithJitter() {
            // act
            val info = QueuePositionInfo(position = 501, estimatedWaitSeconds = 0, token = null)

            // assert
            assertThat(info.recommendedPollIntervalMs).isBetween(2400L, 3600L)
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
