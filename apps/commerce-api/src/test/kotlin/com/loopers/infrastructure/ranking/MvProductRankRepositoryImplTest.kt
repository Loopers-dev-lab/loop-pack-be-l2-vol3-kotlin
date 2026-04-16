package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.MvProductRankMonthly
import com.loopers.domain.ranking.MvProductRankRepository
import com.loopers.domain.ranking.MvProductRankWeekly
import com.loopers.testcontainers.MySqlTestContainersConfig
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.LocalDateTime

@Disabled("MV 리포지토리는 배치 모듈에서 관리. 배치 테스트에서 검증")
@SpringBootTest
@SpringJUnitConfig(MySqlTestContainersConfig::class)
class MvProductRankRepositoryImplTest @Autowired constructor(
    private val repository: MvProductRankRepository,
    private val weeklyJpaRepository: MvProductRankWeeklyJpaRepository,
    private val monthlyJpaRepository: MvProductRankMonthlyJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @BeforeEach
    fun setUp() {
        databaseCleanUp.execute()
    }

    @DisplayName("주간 랭킹 조회: yearWeek로 데이터 조회 및 페이지네이션")
    @Test
    fun shouldFindWeeklyRankingWithPagination() {
        // arrange
        repeat(30) { index ->
            weeklyJpaRepository.save(
                MvProductRankWeekly(
                    productId = (index + 1).toLong(),
                    rank = index + 1,
                    score = (1000 - index).toDouble(),
                    yearWeek = "2026-W15",
                    updatedAt = LocalDateTime.now(),
                ),
            )
        }

        // act: 첫 페이지 (0-19)
        val page1 = repository.findWeeklyRanking("2026-W15", 0, 20)

        // assert
        assertThat(page1).hasSize(20)
        assertThat(page1[0].rank).isEqualTo(1L)
        assertThat(page1[19].rank).isEqualTo(20L)

        // act: 두 번째 페이지 (20-29)
        val page2 = repository.findWeeklyRanking("2026-W15", 1, 20)

        // assert
        assertThat(page2).hasSize(10)
        assertThat(page2[0].rank).isEqualTo(21L)
    }

    @DisplayName("월간 랭킹 조회: yearMonth로 데이터 조회 및 페이지네이션")
    @Test
    fun shouldFindMonthlyRankingWithPagination() {
        // arrange
        repeat(50) { index ->
            monthlyJpaRepository.save(
                MvProductRankMonthly(
                    productId = (index + 1).toLong(),
                    rank = index + 1,
                    score = (1000 - index).toDouble(),
                    yearMonth = "2026-04",
                    updatedAt = LocalDateTime.now(),
                ),
            )
        }

        // act: 첫 페이지
        val page1 = repository.findMonthlyRanking("2026-04", 0, 25)

        // assert
        assertThat(page1).hasSize(25)

        // act: 두 번째 페이지
        val page2 = repository.findMonthlyRanking("2026-04", 1, 25)

        // assert
        assertThat(page2).hasSize(25)
    }

    @DisplayName("주간 카운트: 특정 yearWeek의 데이터 개수 반환")
    @Test
    fun shouldCountWeeklyByYearWeek() {
        // arrange
        repeat(25) { index ->
            weeklyJpaRepository.save(
                MvProductRankWeekly(
                    productId = (index + 1).toLong(),
                    rank = index + 1,
                    score = (1000 - index).toDouble(),
                    yearWeek = "2026-W15",
                    updatedAt = LocalDateTime.now(),
                ),
            )
        }

        // act
        val count = repository.countWeekly("2026-W15")

        // assert
        assertThat(count).isEqualTo(25L)
    }

    @DisplayName("월간 카운트: 특정 yearMonth의 데이터 개수 반환")
    @Test
    fun shouldCountMonthlyByYearMonth() {
        // arrange
        repeat(75) { index ->
            monthlyJpaRepository.save(
                MvProductRankMonthly(
                    productId = (index + 1).toLong(),
                    rank = index + 1,
                    score = (1000 - index).toDouble(),
                    yearMonth = "2026-04",
                    updatedAt = LocalDateTime.now(),
                ),
            )
        }

        // act
        val count = repository.countMonthly("2026-04")

        // assert
        assertThat(count).isEqualTo(75L)
    }

    @DisplayName("존재하지 않는 yearWeek 조회: 빈 리스트 반환")
    @Test
    fun shouldReturnEmptyListForNonExistentYearWeek() {
        // act
        val result = repository.findWeeklyRanking("9999-W99", 0, 20)

        // assert
        assertThat(result).isEmpty()
        assertThat(repository.countWeekly("9999-W99")).isZero()
    }

    @DisplayName("점수 순서 확인: 높은 점수가 낮은 rank를 가짐")
    @Test
    fun shouldOrderByScoreDescending() {
        // arrange
        val products = listOf(
            MvProductRankWeekly(productId = 1L, rank = 1, score = 1000.0, yearWeek = "2026-W15"),
            MvProductRankWeekly(productId = 2L, rank = 2, score = 900.0, yearWeek = "2026-W15"),
            MvProductRankWeekly(productId = 3L, rank = 3, score = 800.0, yearWeek = "2026-W15"),
        )
        products.forEach { weeklyJpaRepository.save(it) }

        // act
        val result = repository.findWeeklyRanking("2026-W15", 0, 10)

        // assert
        assertThat(result[0].score).isEqualTo(1000.0)
        assertThat(result[1].score).isEqualTo(900.0)
        assertThat(result[2].score).isEqualTo(800.0)
    }
}
