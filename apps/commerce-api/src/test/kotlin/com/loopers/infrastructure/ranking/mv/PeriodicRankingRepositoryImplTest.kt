package com.loopers.infrastructure.ranking.mv

import com.loopers.domain.ranking.mv.MonthlyProductRankModel
import com.loopers.domain.ranking.mv.WeeklyProductRankModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import java.time.LocalDate

@DisplayName("PeriodicRankingRepositoryImpl")
class PeriodicRankingRepositoryImplTest {
    private val weeklyJpa: WeeklyProductRankJpaRepository = mockk()
    private val monthlyJpa: MonthlyProductRankJpaRepository = mockk()
    private val impl = PeriodicRankingRepositoryImpl(weeklyJpa, monthlyJpa)

    @DisplayName("주간 랭킹 조회")
    @Nested
    inner class FindTopWeekly {

        @DisplayName("엔티티를 RankedProduct 로 변환해 반환한다")
        @Test
        fun mapsEntityToRankedProduct() {
            // arrange
            val periodStart = LocalDate.of(2026, 4, 13)
            val entities = listOf(
                weeklyEntity(periodStart, rankPosition = 1, productId = 101L, score = 99.9),
                weeklyEntity(periodStart, rankPosition = 2, productId = 202L, score = 88.8),
            )
            every {
                weeklyJpa.findByPeriodStartOrderByRankPositionAsc(periodStart, any())
            } returns entities

            // act
            val result = impl.findTopWeekly(periodStart, offset = 0, limit = 20)

            // assert
            assertThat(result).hasSize(2)
            assertThat(result[0].rank).isEqualTo(1L)
            assertThat(result[0].productId).isEqualTo(101L)
            assertThat(result[0].score).isEqualTo(99.9)
            assertThat(result[1].rank).isEqualTo(2L)
            assertThat(result[1].productId).isEqualTo(202L)
        }

        @DisplayName("offset/limit 을 올바른 PageRequest 로 변환한다")
        @Test
        fun translatesOffsetLimitToPageRequest() {
            val periodStart = LocalDate.of(2026, 4, 13)
            val pageableSlot = slot<Pageable>()
            every {
                weeklyJpa.findByPeriodStartOrderByRankPositionAsc(periodStart, capture(pageableSlot))
            } returns emptyList()

            // 2번째 page, 페이지당 20개 = offset 20
            impl.findTopWeekly(periodStart, offset = 20, limit = 20)

            assertThat(pageableSlot.captured).isEqualTo(PageRequest.of(1, 20))
        }

        @DisplayName("빈 결과가 반환되면 빈 리스트를 내보낸다")
        @Test
        fun returnsEmptyList() {
            val periodStart = LocalDate.of(2026, 4, 13)
            every {
                weeklyJpa.findByPeriodStartOrderByRankPositionAsc(periodStart, any())
            } returns emptyList()

            val result = impl.findTopWeekly(periodStart, offset = 0, limit = 20)

            assertThat(result).isEmpty()
        }

        @DisplayName("offset 이 limit 의 배수가 아니면 예외를 던진다")
        @Test
        fun rejectsInvalidOffset() {
            val periodStart = LocalDate.of(2026, 4, 13)
            assertThatThrownBy {
                impl.findTopWeekly(periodStart, offset = 15, limit = 20)
            }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @DisplayName("주간 랭킹 개수 조회")
    @Nested
    inner class CountWeekly {
        @DisplayName("JPA count 결과를 그대로 반환한다")
        @Test
        fun delegatesToJpa() {
            val periodStart = LocalDate.of(2026, 4, 13)
            every { weeklyJpa.countByPeriodStart(periodStart) } returns 42L

            val result = impl.countWeekly(periodStart)

            assertThat(result).isEqualTo(42L)
            verify(exactly = 1) { weeklyJpa.countByPeriodStart(periodStart) }
        }
    }

    @DisplayName("월간 랭킹 조회")
    @Nested
    inner class FindTopMonthly {
        @DisplayName("엔티티를 RankedProduct 로 변환해 반환한다")
        @Test
        fun mapsEntityToRankedProduct() {
            val yearMonthVal = "2026-04"
            val entities = listOf(
                monthlyEntity(yearMonthVal, rankPosition = 1, productId = 301L, score = 777.7),
            )
            every {
                monthlyJpa.findByYearMonthValOrderByRankPositionAsc(yearMonthVal, any())
            } returns entities

            val result = impl.findTopMonthly(yearMonthVal, offset = 0, limit = 20)

            assertThat(result).hasSize(1)
            assertThat(result[0].rank).isEqualTo(1L)
            assertThat(result[0].productId).isEqualTo(301L)
            assertThat(result[0].score).isEqualTo(777.7)
        }
    }

    @DisplayName("월간 랭킹 개수 조회")
    @Nested
    inner class CountMonthly {
        @DisplayName("JPA count 결과를 그대로 반환한다")
        @Test
        fun delegatesToJpa() {
            val yearMonthVal = "2026-04"
            every { monthlyJpa.countByYearMonthVal(yearMonthVal) } returns 100L

            val result = impl.countMonthly(yearMonthVal)

            assertThat(result).isEqualTo(100L)
            verify(exactly = 1) { monthlyJpa.countByYearMonthVal(yearMonthVal) }
        }
    }

    private fun weeklyEntity(
        periodStart: LocalDate,
        rankPosition: Int,
        productId: Long,
        score: Double,
    ): WeeklyProductRankModel {
        return WeeklyProductRankModel(
            periodStart = periodStart,
            periodEnd = periodStart.plusDays(6),
            rankPosition = rankPosition,
            productId = productId,
            likesCount = 0,
            viewsCount = 0,
            salesCount = 0,
            score = score,
        )
    }

    private fun monthlyEntity(
        yearMonthVal: String,
        rankPosition: Int,
        productId: Long,
        score: Double,
    ): MonthlyProductRankModel {
        return MonthlyProductRankModel(
            yearMonthVal = yearMonthVal,
            periodStart = LocalDate.of(2026, 4, 1),
            periodEnd = LocalDate.of(2026, 4, 30),
            rankPosition = rankPosition,
            productId = productId,
            likesCount = 0,
            viewsCount = 0,
            salesCount = 0,
            score = score,
        )
    }
}
