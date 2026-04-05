package com.loopers.infrastructure.scheduler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("TokenBucket - TPS 수렴 테스트")
class TokenBucketTpsConvergenceTest {

    @DisplayName("최소 1명 발급 정책")
    @Nested
    inner class MinimumOnePolicyTests {
        @Test
        @DisplayName("배치 크기는 매번 최소 1 이상이어야 한다")
        fun `batch_size_is_at_least_1`() {
            // arrange
            val bucket = TokenBucket("test-queue-low-tps", tpsConfig = 1)

            // act & assert: 매번 최소 1개 이상 발급
            repeat(100) {
                val batchSize = bucket.simulateElapsedTimeAndCalculateBatchSize(100)
                assertThat(batchSize).isGreaterThanOrEqualTo(1L)
            }
        }

        @Test
        @DisplayName("TPS=5일 때 100회 실행에서 최소 1 정책 적용")
        fun `tps5_minimum_1_over_10_seconds`() {
            // arrange
            val bucket = TokenBucket("test-queue-5", tpsConfig = 5)
            var totalTokensIssued = 0L
            val runIntervalMs = 100L

            // act: 100ms마다 100회 실행 (10초 모의)
            repeat(100) {
                val batchSize = bucket.simulateElapsedTimeAndCalculateBatchSize(runIntervalMs)
                totalTokensIssued += batchSize
            }

            // assert: 10초에 최소 100명 처리 (100회 × 최소 1)
            assertThat(totalTokensIssued).isGreaterThanOrEqualTo(100L)
        }

        @Test
        @DisplayName("TPS=11일 때 100회 실행에서 최소 1 정책 적용")
        fun `tps11_minimum_1_over_10_seconds`() {
            // arrange
            val bucket = TokenBucket("test-queue-11", tpsConfig = 11)
            var totalTokensIssued = 0L
            val runIntervalMs = 100L

            // act: 100ms마다 100회 실행
            repeat(100) {
                val batchSize = bucket.simulateElapsedTimeAndCalculateBatchSize(runIntervalMs)
                totalTokensIssued += batchSize
            }

            // assert: 최소 100명 처리 (100회 × 최소 1)
            assertThat(totalTokensIssued).isGreaterThanOrEqualTo(100L)
        }

        @Test
        @DisplayName("TPS=175일 때 100회 실행에서 최소 1 정책 적용")
        fun `tps175_minimum_1_over_10_seconds`() {
            // arrange
            val bucket = TokenBucket("test-queue-175", tpsConfig = 175)
            var totalTokensIssued = 0L
            val runIntervalMs = 100L

            // act: 100ms마다 100회 실행
            repeat(100) {
                val batchSize = bucket.simulateElapsedTimeAndCalculateBatchSize(runIntervalMs)
                totalTokensIssued += batchSize
            }

            // assert: 최소 100명 처리 (100회 × 최소 1)
            assertThat(totalTokensIssued).isGreaterThanOrEqualTo(100L)
        }
    }

    @DisplayName("TPS 수렴 검증 (10초 기준)")
    @Nested
    inner class TpsConvergence10SecondsTests {
        @Test
        @DisplayName("TPS=11일 때 10초 동안 110±15명 발급하며 수렴한다")
        fun `tps11_converges_to_11_over_10_seconds`() {
            // arrange: 11 TPS × 10초 = 약 110명 기대
            // 최소 1 정책의 영향을 고려해 tolerance를 ±15명으로 설정
            val bucket = TokenBucket("test-queue-11", tpsConfig = 11)
            var totalTokensIssued = 0L
            val runIntervalMs = 100L
            val expectedTokens = 11.0 * 10.0 // 110
            val tolerance = 15.0 // ±15명 허용

            // act: 100ms마다 100회 실행
            repeat(100) {
                val batchSize = bucket.simulateElapsedTimeAndCalculateBatchSize(runIntervalMs)
                totalTokensIssued += batchSize
            }

            // assert: 예상값 ±tolerance 범위 내
            assertThat(totalTokensIssued.toDouble())
                .isBetween(expectedTokens - tolerance, expectedTokens + tolerance)
        }

        @Test
        @DisplayName("TPS=175일 때 10초 동안 1750±175명 발급하며 수렴한다")
        fun `tps175_converges_to_175_over_10_seconds`() {
            // arrange: 175 TPS × 10초 = 약 1750명 기대
            val bucket = TokenBucket("test-queue-175", tpsConfig = 175)
            var totalTokensIssued = 0L
            val runIntervalMs = 100L
            val expectedTokens = 175.0 * 10.0 // 1750
            val tolerance = 175.0 // ±175명 허용 (±10%)

            // act: 100ms마다 100회 실행
            repeat(100) {
                val batchSize = bucket.simulateElapsedTimeAndCalculateBatchSize(runIntervalMs)
                totalTokensIssued += batchSize
            }

            // assert: 예상값 ±tolerance 범위 내
            assertThat(totalTokensIssued.toDouble())
                .isBetween(expectedTokens - tolerance, expectedTokens + tolerance)
        }
    }

    @DisplayName("TPS 수렴 검증 (1초 기준)")
    @Nested
    inner class TpsConvergence1SecondTests {
        @Test
        @DisplayName("TPS=11일 때 1초 동안 11±5명 발급하며 수렴한다")
        fun `tps11_converges_over_1_second`() {
            // arrange: 11 TPS × 1초 = 약 11명 기대
            // 최소 1 정책의 영향을 고려해 tolerance를 ±5명으로 설정
            val bucket = TokenBucket("test-queue-11-1s", tpsConfig = 11)
            var totalTokensIssued = 0L
            val runIntervalMs = 100L
            val expectedTokens = 11.0 * 1.0 // 11
            val tolerance = 5.0 // ±5명 허용

            // act: 100ms마다 10회 실행
            repeat(10) {
                val batchSize = bucket.simulateElapsedTimeAndCalculateBatchSize(runIntervalMs)
                totalTokensIssued += batchSize
            }

            // assert: 예상값 ±tolerance 범위 내
            assertThat(totalTokensIssued.toDouble())
                .isBetween(expectedTokens - tolerance, expectedTokens + tolerance)
        }

        @Test
        @DisplayName("TPS=175일 때 1초 동안 175±35명 발급하며 수렴한다")
        fun `tps175_converges_over_1_second`() {
            // arrange: 175 TPS × 1초 = 약 175명 기대
            // 단기간이므로 ±35명(±20%) 허용
            val bucket = TokenBucket("test-queue-175-1s", tpsConfig = 175)
            var totalTokensIssued = 0L
            val runIntervalMs = 100L
            val expectedTokens = 175.0 * 1.0 // 175
            val tolerance = 35.0 // ±35명 허용

            // act: 100ms마다 10회 실행
            repeat(10) {
                val batchSize = bucket.simulateElapsedTimeAndCalculateBatchSize(runIntervalMs)
                totalTokensIssued += batchSize
            }

            // assert: 예상값 ±tolerance 범위 내
            assertThat(totalTokensIssued.toDouble())
                .isBetween(expectedTokens - tolerance, expectedTokens + tolerance)
        }
    }

    @DisplayName("분수 토큰 누적")
    @Nested
    inner class FractionalTokenAccumulationTests {
        @Test
        @DisplayName("남은 분수 토큰이 다음 실행으로 누적될 수 있다")
        fun `fractional_tokens_accumulate_for_next_run`() {
            // arrange
            val bucket = TokenBucket("test-queue-fraction", tpsConfig = 5)

            // act: 첫 번째 실행 (100ms 경과)
            // 100ms * (5 / 1000) = 0.5 토큰 → batch=1 (maxOf), leftover=0.5-1=-0.5
            val batch1 = bucket.simulateElapsedTimeAndCalculateBatchSize(100)

            // 두 번째 실행 (100ms 더 경과)
            // -0.5 + 0.5 = 0.0 → batch=1 (maxOf로 최소 1), leftover=0.0-1=-1.0
            val batch2 = bucket.simulateElapsedTimeAndCalculateBatchSize(100)

            // 세 번째 실행 (100ms 더 경과)
            // -1.0 + 0.5 = -0.5 → batch=1 (maxOf), leftover=-0.5-1=-1.5
            val batch3 = bucket.simulateElapsedTimeAndCalculateBatchSize(100)

            // assert: 각 배치는 최소 1명 발급됨 (내부 상태는 검증하지 않음)
            assertThat(batch1).isEqualTo(1L)
            assertThat(batch2).isEqualTo(1L)
            assertThat(batch3).isEqualTo(1L)
        }
    }
}
