package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.repository.WeeklyRankingRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class WeeklyRankingRepositoryImplTest @Autowired constructor(
    private val weeklyRankingRepository: WeeklyRankingRepository,
    private val weeklyRankingJpaRepository: WeeklyRankingJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun entity(
        rankNo: Int,
        productId: Long,
        periodKey: String = "2026-W15",
    ) = MvProductRankWeeklyEntity(
        rankNo = rankNo,
        productId = productId,
        score = rankNo.toDouble() * 100,
        viewCount = 10L,
        likeCount = 5L,
        salesCount = 2L,
        periodKey = periodKey,
        periodStartDate = LocalDate.of(2026, 4, 6),
        periodEndDate = LocalDate.of(2026, 4, 12),
    )

    @Nested
    @DisplayName("findAllByPeriodKey — JPA 스모크")
    inner class FindAllByPeriodKey {

        @Test
        @DisplayName("periodKey별 100건 저장 후 rank_no 오름차순으로 전부 반환된다")
        fun `100건 저장 후 정상 반환`() {
            // Arrange
            val entities = (1..100).map { entity(rankNo = it, productId = it.toLong()) }
            weeklyRankingJpaRepository.saveAll(entities)

            // Act
            val result = weeklyRankingRepository.findAllByPeriodKey("2026-W15")

            // Assert
            assertThat(result).hasSize(100)
            assertThat(result.map { it.rank }).isSortedAccordingTo(compareBy { it })
            assertThat(result[0].rank).isEqualTo(1)
            assertThat(result[99].rank).isEqualTo(100)
        }

        @Test
        @DisplayName("101건 이상 저장되어 있어도 상위 100건만 반환된다")
        fun `101건 이상이면 상위 100건만 반환`() {
            // Arrange
            val entities = (1..120).map { entity(rankNo = it, productId = it.toLong()) }
            weeklyRankingJpaRepository.saveAll(entities)

            // Act
            val result = weeklyRankingRepository.findAllByPeriodKey("2026-W15")

            // Assert
            assertThat(result).hasSize(100)
            assertThat(result[0].rank).isEqualTo(1)
            assertThat(result[99].rank).isEqualTo(100)
        }
    }
}
