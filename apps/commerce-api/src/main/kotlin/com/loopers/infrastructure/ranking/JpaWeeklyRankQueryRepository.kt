package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.RankedProduct
import com.loopers.domain.ranking.WeeklyRankQueryRepository
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.temporal.IsoFields

@Component
class JpaWeeklyRankQueryRepository(
    private val weeklyProductRankJpaRepository: WeeklyProductRankJpaRepository,
    private val rankedProductMapper: RankedProductMapper,
) : WeeklyRankQueryRepository {
    override fun getTopRanked(date: LocalDate, offset: Long, count: Long): List<RankedProduct> {
        if (offset >= MAX_ROWS || count <= 0) return emptyList()
        val year = date.get(IsoFields.WEEK_BASED_YEAR)
        val week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        return weeklyProductRankJpaRepository
            .findByYearAndWeekOrderByRankNumber(year, week)
            .drop(offset.toInt())
            .take(count.toInt())
            .map(rankedProductMapper::toDomain)
    }

    override fun getTotalCount(date: LocalDate): Long {
        val year = date.get(IsoFields.WEEK_BASED_YEAR)
        val week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        return weeklyProductRankJpaRepository.countByYearAndWeek(year, week)
    }

    companion object {
        /**
         * MV는 Top 100 고정 적재. offset이 이 경계를 넘으면 즉시 빈 리스트를 반환해
         * 큰 page 값 요청 시 drop의 Int overflow/음수 변환을 방어한다.
         */
        private const val MAX_ROWS = 100L
    }
}
