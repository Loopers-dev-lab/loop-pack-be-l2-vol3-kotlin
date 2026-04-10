package com.loopers.domain.ranking

import com.loopers.domain.ranking.model.FailedScoreUpdate
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FailedScoreUpdateTest {

    private val validDate: LocalDate = LocalDate.of(2026, 4, 10)

    @Nested
    @DisplayName("생성 시 불변식 검증")
    inner class Invariants {

        @Test
        @DisplayName("정상 파라미터로 생성되면 예외가 발생하지 않고 필드가 그대로 유지된다")
        fun `정상 파라미터 생성 성공`() {
            // Arrange & Act
            val failed = FailedScoreUpdate(
                eventId = "evt-1",
                productId = 1L,
                score = 0.2,
                rankingDate = validDate,
                retryCount = 0,
            )

            // Assert
            assertThat(failed.eventId).isEqualTo("evt-1")
            assertThat(failed.productId).isEqualTo(1L)
            assertThat(failed.score).isEqualTo(0.2)
            assertThat(failed.retryCount).isEqualTo(0)
        }

        @Test
        @DisplayName("retryCount가 음수이면 IllegalArgumentException이 발생한다")
        fun `retryCount 음수 금지`() {
            assertThatThrownBy {
                FailedScoreUpdate(
                    eventId = "evt-1",
                    productId = 1L,
                    score = 0.2,
                    rankingDate = validDate,
                    retryCount = -1,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("retryCount")
        }

        @Test
        @DisplayName("score가 NaN이면 IllegalArgumentException이 발생한다")
        fun `score NaN 금지`() {
            assertThatThrownBy {
                FailedScoreUpdate(
                    eventId = "evt-1",
                    productId = 1L,
                    score = Double.NaN,
                    rankingDate = validDate,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("score")
        }

        @Test
        @DisplayName("score가 Infinity이면 IllegalArgumentException이 발생한다")
        fun `score 무한대 금지`() {
            assertThatThrownBy {
                FailedScoreUpdate(
                    eventId = "evt-1",
                    productId = 1L,
                    score = Double.POSITIVE_INFINITY,
                    rankingDate = validDate,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("score")
        }

        @Test
        @DisplayName("eventId가 공백이면 IllegalArgumentException이 발생한다")
        fun `eventId 공백 금지`() {
            assertThatThrownBy {
                FailedScoreUpdate(
                    eventId = "   ",
                    productId = 1L,
                    score = 0.2,
                    rankingDate = validDate,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("eventId")
        }

        @Test
        @DisplayName("productId가 0 이하이면 IllegalArgumentException이 발생한다")
        fun `productId 0 이하 금지`() {
            assertThatThrownBy {
                FailedScoreUpdate(
                    eventId = "evt-1",
                    productId = 0L,
                    score = 0.2,
                    rankingDate = validDate,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("productId")
        }
    }
}
