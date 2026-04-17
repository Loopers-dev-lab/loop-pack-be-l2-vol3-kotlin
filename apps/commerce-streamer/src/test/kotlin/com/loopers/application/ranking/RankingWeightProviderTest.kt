package com.loopers.application.ranking

import com.loopers.domain.ranking.RankingScoreConfig
import com.loopers.domain.ranking.RankingScoreConfigRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class RankingWeightProviderTest {

    @Mock
    private lateinit var rankingScoreConfigRepository: RankingScoreConfigRepository

    @DisplayName("가중치를 조회할 때,")
    @Nested
    inner class GetWeights {

        @DisplayName("DB에 설정값이 있으면, DB 값을 반환한다.")
        @Test
        fun returnsDbValues_whenConfigExists() {
            // arrange
            val configs = listOf(
                RankingScoreConfig("VIEW_WEIGHT", 0.15, "조회 가중치"),
                RankingScoreConfig("LIKE_WEIGHT", 0.25, "좋아요 가중치"),
                RankingScoreConfig("ORDER_WEIGHT", 0.5, "주문 가중치"),
                RankingScoreConfig("CARRY_OVER_WEIGHT", 0.2, "캐리오버 가중치"),
            )
            whenever(rankingScoreConfigRepository.findAll()).thenReturn(configs)

            val provider = RankingWeightProvider(rankingScoreConfigRepository)
            provider.init()

            // act & assert
            assertAll(
                { assertThat(provider.getViewWeight()).isEqualTo(0.15) },
                { assertThat(provider.getLikeWeight()).isEqualTo(0.25) },
                { assertThat(provider.getOrderWeight()).isEqualTo(0.5) },
                { assertThat(provider.getCarryOverWeight()).isEqualTo(0.2) },
            )
        }

        @DisplayName("DB에 설정값이 없으면, 기본값을 반환한다.")
        @Test
        fun returnsDefaults_whenConfigEmpty() {
            // arrange
            whenever(rankingScoreConfigRepository.findAll()).thenReturn(emptyList())

            val provider = RankingWeightProvider(rankingScoreConfigRepository)
            provider.init()

            // act & assert
            assertAll(
                { assertThat(provider.getViewWeight()).isEqualTo(0.1) },
                { assertThat(provider.getLikeWeight()).isEqualTo(0.2) },
                { assertThat(provider.getOrderWeight()).isEqualTo(0.6) },
                { assertThat(provider.getCarryOverWeight()).isEqualTo(0.1) },
            )
        }

        @DisplayName("DB 조회 실패 시, 기존 값을 유지한다.")
        @Test
        fun retainsPreviousValues_whenDbFails() {
            // arrange — 초기 로드 성공
            val configs = listOf(
                RankingScoreConfig("VIEW_WEIGHT", 0.3, null),
            )
            whenever(rankingScoreConfigRepository.findAll())
                .thenReturn(configs)
                .thenThrow(RuntimeException("DB 연결 실패"))

            val provider = RankingWeightProvider(rankingScoreConfigRepository)
            provider.init()

            // act — 두 번째 refresh 실패
            provider.refresh()

            // assert — 이전 값 유지
            assertThat(provider.getViewWeight()).isEqualTo(0.3)
        }
    }
}
