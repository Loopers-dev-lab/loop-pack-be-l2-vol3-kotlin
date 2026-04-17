package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.MonthlyRankQueryRepository
import com.loopers.domain.ranking.RankedProduct
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class JpaMonthlyRankQueryRepository(
    private val monthlyProductRankJpaRepository: MonthlyProductRankJpaRepository,
    private val rankedProductMapper: RankedProductMapper,
) : MonthlyRankQueryRepository {
    override fun getTopRanked(date: LocalDate, offset: Long, count: Long): List<RankedProduct> {
        if (offset >= MAX_ROWS || count <= 0) return emptyList()
        val year = date.year
        val month = date.monthValue
        return monthlyProductRankJpaRepository
            .findByYearAndMonthOrderByRankNumber(year, month)
            .drop(offset.toInt())
            .take(count.toInt())
            .map(rankedProductMapper::toDomain)
    }

    override fun getTotalCount(date: LocalDate): Long {
        val year = date.year
        val month = date.monthValue
        return monthlyProductRankJpaRepository.countByYearAndMonth(year, month)
    }

    companion object {
        /**
         * MV는 Top 100 고정 적재. offset이 이 경계를 넘으면 즉시 빈 리스트를 반환해
         * 큰 page 값 요청 시 drop의 Int overflow/음수 변환을 방어한다.
         */
        private const val MAX_ROWS = 100L
    }
}
