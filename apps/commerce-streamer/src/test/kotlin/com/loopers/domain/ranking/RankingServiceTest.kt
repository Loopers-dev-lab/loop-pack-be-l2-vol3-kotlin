package com.loopers.domain.ranking

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import java.time.LocalDate
import kotlin.math.log10

@ExtendWith(MockitoExtension::class)
@DisplayName("RankingService")
class RankingServiceTest {

    @Mock
    private lateinit var rankingRepository: RankingRepository

    @InjectMocks
    private lateinit var rankingService: RankingService

    private val today = LocalDate.of(2026, 4, 8)

    @DisplayName("VIEWED 이벤트 처리 시,")
    @Nested
    inner class Viewed {

        @DisplayName("해당 상품 점수를 0.1 증가시킨다.")
        @Test
        fun incrementsScoreBy0_1() {
            // act
            rankingService.updateScoreForView(today, 100L)

            // assert
            verify(rankingRepository).incrementScore(eq(today), eq(100L), eq(0.1))
        }
    }

    @DisplayName("LIKED 이벤트 처리 시,")
    @Nested
    inner class Liked {

        @DisplayName("해당 상품 점수를 0.2 증가시킨다.")
        @Test
        fun incrementsScoreBy0_2() {
            // act
            rankingService.updateScoreForLike(today, 100L)

            // assert
            verify(rankingRepository).incrementScore(eq(today), eq(100L), eq(0.2))
        }
    }

    @DisplayName("UNLIKED 이벤트 처리 시,")
    @Nested
    inner class Unliked {

        @DisplayName("해당 상품 점수를 0.2 차감한다.")
        @Test
        fun decrementsScoreBy0_2() {
            // act
            rankingService.updateScoreForUnlike(today, 100L)

            // assert
            verify(rankingRepository).incrementScore(eq(today), eq(100L), eq(-0.2))
        }
    }

    @DisplayName("ORDER_COMPLETED 이벤트 처리 시,")
    @Nested
    inner class OrderCompleted {

        @DisplayName("상품별 점수를 0.7 × log(unitPrice × quantity)만큼 증가시킨다.")
        @Test
        fun incrementsScoreWithLogFormula() {
            // arrange
            val unitPrice = 10000L
            val quantity = 2

            // act
            rankingService.updateScoreForOrder(today, 100L, unitPrice, quantity)

            // assert
            val expectedScore = 0.7 * log10((unitPrice * quantity).toDouble())
            val scoreCaptor = argumentCaptor<Double>()
            verify(rankingRepository).incrementScore(eq(today), eq(100L), scoreCaptor.capture())
            assertThat(scoreCaptor.firstValue).isCloseTo(expectedScore, org.assertj.core.data.Offset.offset(0.0001))
        }
    }
}
