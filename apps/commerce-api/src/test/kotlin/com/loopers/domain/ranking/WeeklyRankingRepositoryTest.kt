package com.loopers.domain.ranking

import com.loopers.domain.ranking.model.WeeklyProductRank
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class WeeklyRankingRepositoryTest {

    private lateinit var repository: FakeWeeklyRankingRepository

    @BeforeEach
    fun setUp() {
        repository = FakeWeeklyRankingRepository()
    }

    private fun entry(
        rank: Int,
        productId: Long,
        periodKey: String = "2026-W15",
    ) = WeeklyProductRank(
        rank = rank,
        productId = productId,
        score = rank.toDouble() * 100,
        viewCount = 10L,
        likeCount = 5L,
        salesCount = 2L,
        periodKey = periodKey,
        periodStartDate = LocalDate.of(2026, 4, 6),
        periodEndDate = LocalDate.of(2026, 4, 12),
    )

    @Nested
    @DisplayName("findAllByPeriodKey")
    inner class FindAllByPeriodKey {

        @Test
        @DisplayName("rank_no 오름차순으로 최대 100건을 반환한다")
        fun `rank 오름차순 100건 반환`() {
            // Arrange
            (1..100).forEach { repository.addEntry(entry(rank = it, productId = it.toLong())) }

            // Act
            val result = repository.findAllByPeriodKey("2026-W15")

            // Assert
            assertThat(result).hasSize(100)
            assertThat(result.map { it.rank }).isSortedAccordingTo(compareBy { it })
        }

        @Test
        @DisplayName("101건 이상 저장되어 있어도 상위 100건만 반환한다")
        fun `101건 이상이면 상위 100건만 반환`() {
            // Arrange
            (1..120).forEach { repository.addEntry(entry(rank = it, productId = it.toLong())) }

            // Act
            val result = repository.findAllByPeriodKey("2026-W15")

            // Assert
            assertThat(result).hasSize(100)
            assertThat(result[0].rank).isEqualTo(1)
            assertThat(result[99].rank).isEqualTo(100)
        }

        @Test
        @DisplayName("존재하지 않는 periodKey는 빈 목록을 반환한다")
        fun `존재하지 않는 periodKey 빈 목록 반환`() {
            // Act
            val result = repository.findAllByPeriodKey("2026-W99")

            // Assert
            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("동일 periodKey에 100건 이하만 저장되어 있으면 그 수만큼 반환한다")
        fun `100건 이하면 저장된 수만큼 반환`() {
            // Arrange
            (1..50).forEach { repository.addEntry(entry(rank = it, productId = it.toLong())) }

            // Act
            val result = repository.findAllByPeriodKey("2026-W15")

            // Assert
            assertThat(result).hasSize(50)
        }

        @Test
        @DisplayName("다른 periodKey의 데이터는 포함하지 않는다")
        fun `다른 periodKey 격리`() {
            // Arrange
            repository.addEntry(entry(rank = 1, productId = 1L, periodKey = "2026-W15"))
            repository.addEntry(entry(rank = 1, productId = 2L, periodKey = "2026-W14"))

            // Act
            val result = repository.findAllByPeriodKey("2026-W15")

            // Assert
            assertThat(result).hasSize(1)
            assertThat(result[0].productId).isEqualTo(1L)
        }
    }
}
