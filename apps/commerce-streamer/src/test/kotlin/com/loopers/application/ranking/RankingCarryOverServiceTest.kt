package com.loopers.application.ranking

import com.loopers.infrastructure.ranking.RankingRedisRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class RankingCarryOverServiceTest {

    @Mock
    private lateinit var rankingRedisRepository: RankingRedisRepository

    @InjectMocks
    private lateinit var rankingCarryOverService: RankingCarryOverService

    @DisplayName("Score Carry-Over를 실행할 때,")
    @Nested
    inner class ExecuteCarryOver {

        @DisplayName("전일 점수의 10%를 다음 날 키에 복사한다.")
        @Test
        fun copiesYesterdayScoreToTomorrow() {
            // arrange
            val today = LocalDate.of(2026, 4, 8)
            val tomorrow = today.plusDays(1)

            // act
            rankingCarryOverService.carryOver(today)

            // assert
            verify(rankingRedisRepository).carryOverScores(today, tomorrow, CARRY_OVER_WEIGHT)
        }
    }

    companion object {
        private const val CARRY_OVER_WEIGHT = 0.1
    }
}
