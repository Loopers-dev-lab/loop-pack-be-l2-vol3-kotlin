package com.loopers.domain.ranking

import com.loopers.infrastructure.ranking.RankingRedisRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RankingAggregationServiceTest {
    private lateinit var rankingEventRepository: RankingEventRepository
    private lateinit var rankingMetricRepository: RankingMetricRepository
    private lateinit var rankingRedisRepository: RankingRedisRepository
    private lateinit var service: RankingAggregationService

    private val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))

    @BeforeEach
    fun setUp() {
        rankingEventRepository = mock()
        rankingMetricRepository = mock()
        rankingRedisRepository = mock()
        service = RankingAggregationService(
            rankingEventRepository,
            rankingMetricRepository,
            rankingRedisRepository,
        )
    }

    @DisplayName("집계를 수행할 때, ")
    @Nested
    inner class Aggregate {
        @DisplayName("미집계 이벤트가 없으면 아무 작업도 하지 않는다.")
        @Test
        fun doesNothing_whenNoUnaggregatedEvents() {
            // arrange
            whenever(rankingEventRepository.aggregateUnaggregated()).thenReturn(emptyList())

            // act
            service.aggregate()

            // assert
            verify(rankingMetricRepository, never()).save(any())
            verify(rankingRedisRepository, never()).replaceAll(any(), any())
        }

        @DisplayName("미집계 이벤트를 metric에 합산하고 Redis에 동기화한다.")
        @Test
        fun aggregatesAndSyncs() {
            // arrange
            val aggregated = listOf(
                AggregatedScore(productId = 1L, rankingDate = today, totalScore = 100.0, count = 10),
                AggregatedScore(productId = 2L, rankingDate = today, totalScore = 50.0, count = 5),
            )
            whenever(rankingEventRepository.aggregateUnaggregated()).thenReturn(aggregated)
            whenever(rankingMetricRepository.findByProductIdAndRankingDate(any(), any())).thenReturn(null)
            whenever(rankingMetricRepository.save(any())).thenAnswer { it.arguments[0] }
            whenever(rankingMetricRepository.findAllByRankingDate(today)).thenReturn(
                listOf(
                    RankingMetric(productId = 1L, rankingDate = today, totalScore = 100.0, eventCount = 10),
                    RankingMetric(productId = 2L, rankingDate = today, totalScore = 50.0, eventCount = 5),
                ),
            )

            // act
            service.aggregate()

            // assert
            verify(rankingEventRepository).markAllAggregated()

            val scoresCaptor = argumentCaptor<Map<Long, Double>>()
            verify(rankingRedisRepository).replaceAll(eq(today), scoresCaptor.capture())
            val scores = scoresCaptor.firstValue
            assertThat(scores).hasSize(2)
            assertThat(scores[1L]).isEqualTo(100.0)
            assertThat(scores[2L]).isEqualTo(50.0)
        }

        @DisplayName("기존 metric이 있으면 점수를 합산한다.")
        @Test
        fun addsToExistingMetric() {
            // arrange
            val aggregated = listOf(
                AggregatedScore(productId = 1L, rankingDate = today, totalScore = 50.0, count = 5),
            )
            val existingMetric = RankingMetric(productId = 1L, rankingDate = today, totalScore = 100.0, eventCount = 10)
            whenever(rankingEventRepository.aggregateUnaggregated()).thenReturn(aggregated)
            whenever(rankingMetricRepository.findByProductIdAndRankingDate(1L, today)).thenReturn(existingMetric)
            whenever(rankingMetricRepository.save(any())).thenAnswer { it.arguments[0] }
            whenever(rankingMetricRepository.findAllByRankingDate(today)).thenReturn(listOf(existingMetric))

            // act
            service.aggregate()

            // assert
            val metricCaptor = argumentCaptor<RankingMetric>()
            verify(rankingMetricRepository).save(metricCaptor.capture())
            assertThat(metricCaptor.firstValue.totalScore).isEqualTo(150.0)
            assertThat(metricCaptor.firstValue.eventCount).isEqualTo(15)
        }
    }

    @DisplayName("Redis 동기화할 때, ")
    @Nested
    inner class SyncToRedis {
        @DisplayName("metric이 없으면 Redis에 쓰지 않는다.")
        @Test
        fun doesNotSync_whenNoMetrics() {
            // arrange
            whenever(rankingMetricRepository.findAllByRankingDate(today)).thenReturn(emptyList())

            // act
            service.syncToRedis(today)

            // assert
            verify(rankingRedisRepository, never()).replaceAll(any(), any())
        }
    }
}
