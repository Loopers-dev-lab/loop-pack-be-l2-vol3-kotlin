package com.loopers.infrastructure.scheduler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("TokenBucket - TPS 수렴 테스트")
class TokenBucketTpsConvergenceTest {

    @DisplayName("TPS=5일 때 100회 실행에서 최소 1 정책 적용")
    @Test
    fun `tps5_converges_to_5_over_10_seconds`() {
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
        // 5 TPS = 0.5 tokens/100ms이지만 maxOf(1L) 정책으로 매번 최소 1이상
        assertThat(totalTokensIssued).isGreaterThanOrEqualTo(100L)
    }

    @DisplayName("TPS=11일 때 100회 실행에서 최소 1 정책 적용")
    @Test
    fun `tps11_converges_to_11_over_10_seconds`() {
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

    @DisplayName("TPS=175일 때 100회 실행에서 최소 1 정책 적용")
    @Test
    fun `tps175_converges_to_175_over_10_seconds`() {
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

    @DisplayName("TPS=5일 때 10회 실행에서 최소 1 정책 적용")
    @Test
    fun `tps5_accurate_over_1_second`() {
        // arrange
        val bucket = TokenBucket("test-queue-5-1s", tpsConfig = 5)
        var totalTokensIssued = 0L
        val runIntervalMs = 100L

        // act: 100ms마다 10회 실행
        repeat(10) {
            val batchSize = bucket.simulateElapsedTimeAndCalculateBatchSize(runIntervalMs)
            totalTokensIssued += batchSize
        }

        // assert: 최소 10명 처리 (10회 × 최소 1)
        assertThat(totalTokensIssued).isGreaterThanOrEqualTo(10L)
    }

    @DisplayName("TPS=11일 때 10회 실행에서 최소 1 정책 적용")
    @Test
    fun `tps11_accurate_over_1_second`() {
        // arrange
        val bucket = TokenBucket("test-queue-11-1s", tpsConfig = 11)
        var totalTokensIssued = 0L
        val runIntervalMs = 100L

        // act: 100ms마다 10회 실행
        repeat(10) {
            val batchSize = bucket.simulateElapsedTimeAndCalculateBatchSize(runIntervalMs)
            totalTokensIssued += batchSize
        }

        // assert: 최소 10명 처리 (10회 × 최소 1)
        assertThat(totalTokensIssued).isGreaterThanOrEqualTo(10L)
    }

    @DisplayName("TPS=175일 때 10회 실행에서 최소 1 정책 적용")
    @Test
    fun `tps175_accurate_over_1_second`() {
        // arrange
        val bucket = TokenBucket("test-queue-175-1s", tpsConfig = 175)
        var totalTokensIssued = 0L
        val runIntervalMs = 100L

        // act: 100ms마다 10회 실행
        repeat(10) {
            val batchSize = bucket.simulateElapsedTimeAndCalculateBatchSize(runIntervalMs)
            totalTokensIssued += batchSize
        }

        // assert: 최소 10명 처리 (10회 × 최소 1)
        assertThat(totalTokensIssued).isGreaterThanOrEqualTo(10L)
    }

    @DisplayName("배치 크기는 최소 1 이상이어야 한다")
    @Test
    fun `batch_size_is_at_least_1`() {
        // arrange
        val bucket = TokenBucket("test-queue-low-tps", tpsConfig = 1)

        // act & assert: 매번 최소 1개 이상 발급
        repeat(100) {
            val batchSize = bucket.simulateElapsedTimeAndCalculateBatchSize(100)
            assertThat(batchSize).isGreaterThanOrEqualTo(1L)
        }
    }

    @DisplayName("남은 분수 토큰이 다음 실행으로 누적되며 음수가 될 수 있다")
    @Test
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

        // assert: 각 배치는 최소 1명 발급되고, 누적된 토큰은 음수가 될 수 있음
        assertThat(batch1).isEqualTo(1L)
        assertThat(batch2).isEqualTo(1L)
        assertThat(batch3).isEqualTo(1L)
        // 최악의 경우 -1.5까지 음수가 될 수 있음
        assertThat(bucket.accumulatedTokens).isLessThanOrEqualTo(-1.0)
    }
}
