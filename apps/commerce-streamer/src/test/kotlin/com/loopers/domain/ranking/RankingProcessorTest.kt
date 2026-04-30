package com.loopers.domain.ranking

import com.loopers.domain.metrics.FakeProductMetricsRepository
import com.loopers.domain.metrics.ProductMetrics
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class RankingProcessorTest {

    private lateinit var metricsRepository: FakeProductMetricsRepository
    private lateinit var rankingRepository: FakeRankingRepository
    private lateinit var weightsRepository: FakeRankingWeightsRepository
    private lateinit var rankingProcessor: RankingProcessor

    private val now get() = LocalDateTime.now()
    private val today get() = LocalDate.now()
    private val todayKey get() = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    private val hourKey get() = now.format(DateTimeFormatter.ofPattern("yyyyMMddHH"))

    @BeforeEach
    fun setUp() {
        metricsRepository = FakeProductMetricsRepository()
        rankingRepository = FakeRankingRepository()
        weightsRepository = FakeRankingWeightsRepository()
        rankingProcessor = RankingProcessor(
            productMetricsRepository = metricsRepository,
            rankingRepository = rankingRepository,
            rankingWeightsRepository = weightsRepository,
        )
    }

    @Nested
    @DisplayName("updateRankings")
    inner class UpdateRankings {

        @Test
        @DisplayName("DEFAULT 가중치로 metrics 기반 score를 계산하여 ZSET에 갱신한다")
        fun updatesScoresFromMetricsWithDefaultWeights() {
            // arrange — 가중치 저장소가 비어있으면 DEFAULT fallback
            val metrics1 = ProductMetrics(productId = 1L, metricDate = today).apply {
                repeat(100) { incrementViewCount() }
                repeat(10) { incrementLikeCount() }
                repeat(5) { incrementOrderCount() }
            }
            val metrics2 = ProductMetrics(productId = 2L, metricDate = today).apply {
                repeat(50) { incrementViewCount() }
                repeat(20) { incrementLikeCount() }
                repeat(3) { incrementOrderCount() }
            }
            metricsRepository.save(metrics1)
            metricsRepository.save(metrics2)

            // act
            rankingProcessor.updateRankings(setOf(1L, 2L))

            // assert
            // product1: (100*0.1) + (10*0.2) + (5*0.7) = 10 + 2 + 3.5 = 15.5
            assertThat(rankingRepository.getScore(todayKey, 1L))
                .isCloseTo(15.5, Offset.offset(0.001))
            // product2: (50*0.1) + (20*0.2) + (3*0.7) = 5 + 4 + 2.1 = 11.1
            assertThat(rankingRepository.getScore(todayKey, 2L))
                .isCloseTo(11.1, Offset.offset(0.001))
            // 시간 키도 동일한 score로 갱신되어야 함
            assertThat(rankingRepository.getScore(hourKey, 1L))
                .isCloseTo(15.5, Offset.offset(0.001))
            assertThat(rankingRepository.getScore(hourKey, 2L))
                .isCloseTo(11.1, Offset.offset(0.001))
        }

        @Test
        @DisplayName("저장된 가중치가 있으면 해당 값을 사용하여 score를 계산한다")
        fun usesStoredWeightsWhenAvailable() {
            // arrange
            weightsRepository.setWeights(
                RankingWeights(view = 0.5, like = 0.3, order = 0.2),
            )
            val metrics = ProductMetrics(productId = 1L, metricDate = today).apply {
                repeat(100) { incrementViewCount() }
                repeat(50) { incrementLikeCount() }
                repeat(10) { incrementOrderCount() }
            }
            metricsRepository.save(metrics)

            // act
            rankingProcessor.updateRankings(setOf(1L))

            // assert — (100 * 0.5) + (50 * 0.3) + (10 * 0.2) = 50 + 15 + 2 = 67.0
            assertThat(rankingRepository.getScore(todayKey, 1L))
                .isCloseTo(67.0, Offset.offset(0.001))
        }

        @Test
        @DisplayName("빈 productId Set이면 아무 작업도 하지 않는다")
        fun emptyProductIdsDoesNothing() {
            // act
            rankingProcessor.updateRankings(emptySet())

            // assert
            assertThat(rankingRepository.getScores(todayKey)).isEmpty()
            assertThat(rankingRepository.getScores(hourKey)).isEmpty()
        }

        @Test
        @DisplayName("metrics가 없는 productId는 무시된다")
        fun ignoresMissingMetrics() {
            // arrange
            val metrics1 = ProductMetrics(productId = 1L, metricDate = today).apply {
                repeat(10) { incrementViewCount() }
            }
            metricsRepository.save(metrics1)

            // act
            rankingProcessor.updateRankings(setOf(1L, 99L))

            // assert
            assertThat(rankingRepository.getScore(todayKey, 1L)).isNotNull()
            assertThat(rankingRepository.getScore(todayKey, 99L)).isNull()
        }
    }
}
