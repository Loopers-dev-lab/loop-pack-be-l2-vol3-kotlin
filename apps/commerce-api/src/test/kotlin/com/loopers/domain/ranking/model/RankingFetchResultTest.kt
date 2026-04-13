package com.loopers.domain.ranking.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RankingFetchResultTest {

    @Nested
    @DisplayName("생성 시 불변식 검증")
    inner class Invariants {

        @Test
        @DisplayName("rawFetchCount가 entries.size와 같거나 크면 정상 생성된다")
        fun `정상 생성`() {
            // Arrange
            val entries = listOf(
                RankingEntry(productId = 1L, score = 10.0),
                RankingEntry(productId = 2L, score = 5.0),
            )

            // Act
            val result = RankingFetchResult(entries = entries, rawFetchCount = 3)

            // Assert — 파싱 드랍 1건(rawFetchCount=3, entries=2)
            assertThat(result.entries).hasSize(2)
            assertThat(result.rawFetchCount).isEqualTo(3)
        }

        @Test
        @DisplayName("rawFetchCount가 음수이면 IllegalArgumentException이 발생한다")
        fun `rawFetchCount 음수 금지`() {
            assertThatThrownBy {
                RankingFetchResult(entries = emptyList(), rawFetchCount = -1)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("rawFetchCount")
        }

        @Test
        @DisplayName("rawFetchCount가 entries.size보다 작으면 IllegalArgumentException이 발생한다")
        fun `rawFetchCount가 entries size보다 작으면 금지`() {
            val entries = listOf(
                RankingEntry(productId = 1L, score = 10.0),
                RankingEntry(productId = 2L, score = 5.0),
            )

            assertThatThrownBy {
                RankingFetchResult(entries = entries, rawFetchCount = 1)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("rawFetchCount")
        }
    }
}
