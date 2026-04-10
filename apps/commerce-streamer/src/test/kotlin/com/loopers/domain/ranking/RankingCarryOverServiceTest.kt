package com.loopers.domain.ranking

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RankingCarryOverServiceTest {

    private lateinit var rankingRepository: FakeRankingRepository
    private lateinit var rankingCarryOverService: RankingCarryOverService

    private val dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd")

    @BeforeEach
    fun setUp() {
        rankingRepository = FakeRankingRepository()
        rankingCarryOverService = RankingCarryOverService(rankingRepository)
    }

    @Nested
    @DisplayName("carryOverToNextDay")
    inner class CarryOverToNextDay {

        @Test
        @DisplayName("전일 점수에 0.1을 곱해 다음 날 키로 복사한다")
        fun carriesOverWithDecayWeight() {
            // arrange
            val today = LocalDate.of(2026, 4, 9)
            val todayKey = today.format(dateFormat)
            val tomorrowKey = today.plusDays(1).format(dateFormat)

            rankingRepository.updateScores(
                dateKey = todayKey,
                hourKey = "${todayKey}14",
                scores = mapOf(1L to 100.0, 2L to 50.0, 3L to 10.0),
            )

            // act
            rankingCarryOverService.carryOverToNextDay(today)

            // assert — 10% 감쇠
            assertThat(rankingRepository.getScore(tomorrowKey, 1L))
                .isCloseTo(10.0, Offset.offset(0.001))
            assertThat(rankingRepository.getScore(tomorrowKey, 2L))
                .isCloseTo(5.0, Offset.offset(0.001))
            assertThat(rankingRepository.getScore(tomorrowKey, 3L))
                .isCloseTo(1.0, Offset.offset(0.001))
        }

        @Test
        @DisplayName("전일 랭킹이 비어있으면 다음 날 키도 생성되지 않는다")
        fun emptyPreviousDayDoesNothing() {
            // arrange
            val today = LocalDate.of(2026, 4, 9)
            val tomorrowKey = today.plusDays(1).format(dateFormat)

            // act
            rankingCarryOverService.carryOverToNextDay(today)

            // assert
            assertThat(rankingRepository.getScores(tomorrowKey)).isEmpty()
        }

        @Test
        @DisplayName("기존 점수 순위가 carry-over 후에도 유지된다")
        fun ordersArePreservedAfterCarryOver() {
            // arrange
            val today = LocalDate.of(2026, 4, 9)
            val todayKey = today.format(dateFormat)
            val tomorrowKey = today.plusDays(1).format(dateFormat)

            rankingRepository.updateScores(
                dateKey = todayKey,
                hourKey = "${todayKey}15",
                scores = mapOf(1L to 100.0, 2L to 50.0, 3L to 200.0),
            )

            // act
            rankingCarryOverService.carryOverToNextDay(today)

            // assert — 3등 > 1등 > 2등 순서 유지
            val scores = rankingRepository.getScores(tomorrowKey)
            assertThat(scores[3L]).isGreaterThan(scores[1L]!!)
            assertThat(scores[1L]).isGreaterThan(scores[2L]!!)
        }
    }
}
