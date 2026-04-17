package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.WeeklyRankQueryRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@DisplayName("JpaWeeklyRankQueryRepository 통합 테스트")
@SpringBootTest
class JpaWeeklyRankQueryRepositoryIntegrationTest
    @Autowired
    constructor(
        private val weeklyRankQueryRepository: WeeklyRankQueryRepository,
        private val weeklyProductRankJpaRepository: WeeklyProductRankJpaRepository,
        private val databaseCleanUp: DatabaseCleanUp,
    ) {
        @AfterEach
        fun tearDown() {
            databaseCleanUp.truncateAllTables()
        }

        @Nested
        @DisplayName("getTopRanked — (year, week)로 MV 조회, rank_num 오름차순")
        inner class GetTopRanked {

            @Test
            @DisplayName("offset=0, count=3 → rank 1~3 반환")
            fun getTopRanked_firstPage() {
                // arrange: 2026-W16 MV 데이터
                save(productId = 100, year = 2026, week = 16, rankNumber = 1, totalScore = 10.5)
                save(productId = 101, year = 2026, week = 16, rankNumber = 2, totalScore = 8.3)
                save(productId = 102, year = 2026, week = 16, rankNumber = 3, totalScore = 5.0)
                save(productId = 103, year = 2026, week = 16, rankNumber = 4, totalScore = 3.2)

                // 2026-04-16 → ISO week 16
                val result = weeklyRankQueryRepository.getTopRanked(LocalDate.of(2026, 4, 16), 0, 3)

                assertAll(
                    { assertThat(result).hasSize(3) },
                    { assertThat(result[0].productId).isEqualTo(100L) },
                    { assertThat(result[0].rank).isEqualTo(1L) },
                    { assertThat(result[0].score).isEqualTo(10.5) },
                    { assertThat(result[2].productId).isEqualTo(102L) },
                    { assertThat(result[2].rank).isEqualTo(3L) },
                )
            }

            @Test
            @DisplayName("offset=3, count=3 → rank 4부터 남은 결과만 반환 (짧은 페이지)")
            fun getTopRanked_secondPage() {
                save(productId = 100, year = 2026, week = 16, rankNumber = 1, totalScore = 10.5)
                save(productId = 101, year = 2026, week = 16, rankNumber = 2, totalScore = 8.3)
                save(productId = 102, year = 2026, week = 16, rankNumber = 3, totalScore = 5.0)
                save(productId = 103, year = 2026, week = 16, rankNumber = 4, totalScore = 3.2)

                val result = weeklyRankQueryRepository.getTopRanked(LocalDate.of(2026, 4, 16), 3, 3)

                assertAll(
                    { assertThat(result).hasSize(1) },
                    { assertThat(result[0].productId).isEqualTo(103L) },
                    { assertThat(result[0].rank).isEqualTo(4L) },
                )
            }

            @Test
            @DisplayName("데이터 없는 주 → 빈 리스트")
            fun getTopRanked_emptyWeek() {
                val result = weeklyRankQueryRepository.getTopRanked(LocalDate.of(2099, 1, 1), 0, 10)

                assertThat(result).isEmpty()
            }

            @Test
            @DisplayName("offset이 MV 최대(100)을 넘으면 즉시 빈 리스트 반환 (overflow 가드)")
            fun getTopRanked_offsetBeyondMaxRows() {
                save(productId = 100, year = 2026, week = 16, rankNumber = 1, totalScore = 10.5)

                val result = weeklyRankQueryRepository.getTopRanked(LocalDate.of(2026, 4, 16), 100, 10)

                assertThat(result).isEmpty()
            }

            @Test
            @DisplayName("ISO week 경계(2024-12-30)는 week-based year 2025, week 1로 조회된다")
            fun getTopRanked_isoWeekBoundary() {
                // 2024-12-30은 ISO WEEK_BASED_YEAR=2025, WEEK=1
                save(productId = 777, year = 2025, week = 1, rankNumber = 1, totalScore = 42.0)
                // 비교용: calendar 기준 2024가 아니라는 증명을 위해 (2024, 52) 데이터도 삽입
                save(productId = 888, year = 2024, week = 52, rankNumber = 1, totalScore = 99.0)

                val result = weeklyRankQueryRepository.getTopRanked(LocalDate.of(2024, 12, 30), 0, 10)

                assertAll(
                    { assertThat(result).hasSize(1) },
                    { assertThat(result[0].productId).isEqualTo(777L) },
                )
            }
        }

        @Nested
        @DisplayName("getTotalCount — (year, week) 행 수")
        inner class GetTotalCount {

            @Test
            @DisplayName("해당 주 MV 행이 있으면 그 행 수를 반환")
            fun getTotalCount_hasRows() {
                save(productId = 100, year = 2026, week = 16, rankNumber = 1, totalScore = 10.5)
                save(productId = 101, year = 2026, week = 16, rankNumber = 2, totalScore = 8.3)

                val count = weeklyRankQueryRepository.getTotalCount(LocalDate.of(2026, 4, 16))

                assertThat(count).isEqualTo(2L)
            }

            @Test
            @DisplayName("해당 주 MV 행이 없으면 0 반환")
            fun getTotalCount_empty() {
                val count = weeklyRankQueryRepository.getTotalCount(LocalDate.of(2099, 1, 1))

                assertThat(count).isEqualTo(0L)
            }
        }

        private fun save(
            productId: Long,
            year: Int,
            week: Int,
            rankNumber: Int,
            totalScore: Double,
        ) {
            weeklyProductRankJpaRepository.save(
                WeeklyProductRankEntity(
                    productId = productId,
                    year = year,
                    week = week,
                    totalScore = totalScore,
                    rankNumber = rankNumber,
                    viewCount = 0,
                    likeCount = 0,
                    unitsSold = 0,
                    salesAmount = 0L,
                    orderScore = 0.0,
                ),
            )
        }
    }
