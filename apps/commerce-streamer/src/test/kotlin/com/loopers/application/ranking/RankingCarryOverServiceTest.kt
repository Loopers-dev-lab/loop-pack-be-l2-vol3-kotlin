package com.loopers.application.ranking

import com.loopers.domain.ranking.RankingRedisOperations
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class RankingCarryOverServiceTest {

    @Mock
    private lateinit var rankingRedisOperations: RankingRedisOperations

    @Mock
    private lateinit var rankingWeightProvider: RankingWeightProvider

    @InjectMocks
    private lateinit var rankingCarryOverService: RankingCarryOverService

    @DisplayName("Score Carry-Over를 실행할 때,")
    @Nested
    inner class ExecuteCarryOver {

        @DisplayName("WeightProvider에서 carry-over 가중치를 가져와 적용한다.")
        @Test
        fun usesWeightFromProvider() {
            // arrange
            val today = LocalDate.of(2026, 4, 8)
            val tomorrow = today.plusDays(1)
            whenever(rankingWeightProvider.getCarryOverWeight()).thenReturn(0.15)

            // act
            rankingCarryOverService.carryOver(today)

            // assert
            verify(rankingWeightProvider).getCarryOverWeight()
            verify(rankingRedisOperations).carryOverScores(today, tomorrow, 0.15)
        }
    }
}
