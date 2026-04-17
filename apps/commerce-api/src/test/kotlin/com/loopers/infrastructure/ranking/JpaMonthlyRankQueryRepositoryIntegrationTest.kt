package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.MonthlyRankQueryRepository
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

@DisplayName("JpaMonthlyRankQueryRepository 통합 테스트")
@SpringBootTest
class JpaMonthlyRankQueryRepositoryIntegrationTest
    @Autowired
    constructor(
        private val monthlyRankQueryRepository: MonthlyRankQueryRepository,
        private val monthlyProductRankJpaRepository: MonthlyProductRankJpaRepository,
        private val databaseCleanUp: DatabaseCleanUp,
    ) {
        @AfterEach
        fun tearDown() {
            databaseCleanUp.truncateAllTables()
        }

        @Nested
        @DisplayName("getTopRanked — (year, month)로 MV 조회, rank_num 오름차순")
        inner class GetTopRanked {

            @Test
            @DisplayName("offset=0, count=3 → rank 1~3 반환")
            fun getTopRanked_firstPage() {
                save(productId = 100, year = 2026, month = 4, rankNumber = 1, totalScore = 10.5)
                save(productId = 101, year = 2026, month = 4, rankNumber = 2, totalScore = 8.3)
                save(productId = 102, year = 2026, month = 4, rankNumber = 3, totalScore = 5.0)
                save(productId = 103, year = 2026, month = 4, rankNumber = 4, totalScore = 3.2)

                val result = monthlyRankQueryRepository.getTopRanked(LocalDate.of(2026, 4, 16), 0, 3)

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
            @DisplayName("데이터 없는 월 → 빈 리스트")
            fun getTopRanked_emptyMonth() {
                val result = monthlyRankQueryRepository.getTopRanked(LocalDate.of(2099, 1, 1), 0, 10)

                assertThat(result).isEmpty()
            }

            @Test
            @DisplayName("offset이 MV 최대(100)을 넘으면 즉시 빈 리스트 반환 (overflow 가드)")
            fun getTopRanked_offsetBeyondMaxRows() {
                save(productId = 100, year = 2026, month = 4, rankNumber = 1, totalScore = 10.5)

                val result = monthlyRankQueryRepository.getTopRanked(LocalDate.of(2026, 4, 16), 100, 10)

                assertThat(result).isEmpty()
            }

            @Test
            @DisplayName("다른 (year, month) 데이터는 결과에 포함되지 않는다")
            fun getTopRanked_scopedToMonth() {
                save(productId = 100, year = 2026, month = 4, rankNumber = 1, totalScore = 10.5)
                save(productId = 900, year = 2026, month = 3, rankNumber = 1, totalScore = 99.0)
                save(productId = 901, year = 2025, month = 4, rankNumber = 1, totalScore = 99.0)

                val result = monthlyRankQueryRepository.getTopRanked(LocalDate.of(2026, 4, 16), 0, 10)

                assertAll(
                    { assertThat(result).hasSize(1) },
                    { assertThat(result[0].productId).isEqualTo(100L) },
                )
            }
        }

        @Nested
        @DisplayName("getTotalCount — (year, month) 행 수")
        inner class GetTotalCount {

            @Test
            @DisplayName("해당 월 MV 행이 있으면 그 행 수를 반환")
            fun getTotalCount_hasRows() {
                save(productId = 100, year = 2026, month = 4, rankNumber = 1, totalScore = 10.5)
                save(productId = 101, year = 2026, month = 4, rankNumber = 2, totalScore = 8.3)

                val count = monthlyRankQueryRepository.getTotalCount(LocalDate.of(2026, 4, 16))

                assertThat(count).isEqualTo(2L)
            }

            @Test
            @DisplayName("해당 월 MV 행이 없으면 0 반환")
            fun getTotalCount_empty() {
                val count = monthlyRankQueryRepository.getTotalCount(LocalDate.of(2099, 1, 1))

                assertThat(count).isEqualTo(0L)
            }
        }

        private fun save(
            productId: Long,
            year: Int,
            month: Int,
            rankNumber: Int,
            totalScore: Double,
        ) {
            monthlyProductRankJpaRepository.save(
                MonthlyProductRankEntity(
                    productId = productId,
                    year = year,
                    month = month,
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
