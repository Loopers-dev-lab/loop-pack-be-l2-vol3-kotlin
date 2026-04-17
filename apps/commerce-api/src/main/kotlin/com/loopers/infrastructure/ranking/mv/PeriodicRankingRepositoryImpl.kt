package com.loopers.infrastructure.ranking.mv

import com.loopers.domain.ranking.PeriodicRankingRepository
import com.loopers.domain.ranking.RankedProduct
import com.loopers.domain.ranking.mv.MonthlyProductRankModel
import com.loopers.domain.ranking.mv.WeeklyProductRankModel
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class PeriodicRankingRepositoryImpl(
    private val weeklyJpaRepository: WeeklyProductRankJpaRepository,
    private val monthlyJpaRepository: MonthlyProductRankJpaRepository,
) : PeriodicRankingRepository {

    override fun findTopWeekly(periodStart: LocalDate, offset: Long, limit: Long): List<RankedProduct> {
        val pageable = toPageable(offset, limit)
        return weeklyJpaRepository
            .findByPeriodStartOrderByRankPositionAsc(periodStart, pageable)
            .map { it.toRankedProduct() }
    }

    override fun countWeekly(periodStart: LocalDate): Long {
        return weeklyJpaRepository.countByPeriodStart(periodStart)
    }

    override fun findTopMonthly(yearMonthVal: String, offset: Long, limit: Long): List<RankedProduct> {
        val pageable = toPageable(offset, limit)
        return monthlyJpaRepository
            .findByYearMonthValOrderByRankPositionAsc(yearMonthVal, pageable)
            .map { it.toRankedProduct() }
    }

    override fun countMonthly(yearMonthVal: String): Long {
        return monthlyJpaRepository.countByYearMonthVal(yearMonthVal)
    }

    /**
     * 오프셋/리미트 인터페이스를 JPA [PageRequest] 로 변환한다.
     * 호출자(Service) 가 `offset = (page - 1) * size` 를 유지하므로 [offset] 은 항상 [limit] 의 배수다.
     */
    private fun toPageable(offset: Long, limit: Long): PageRequest {
        require(limit > 0) { "limit 은 0 보다 커야 한다" }
        require(offset >= 0 && offset % limit == 0L) { "offset 은 limit 의 배수여야 한다" }
        val pageNumber = (offset / limit).toInt()
        val pageSize = limit.toInt()
        return PageRequest.of(pageNumber, pageSize)
    }

    private fun WeeklyProductRankModel.toRankedProduct(): RankedProduct =
        RankedProduct(rank = this.rankPosition.toLong(), productId = this.productId, score = this.score)

    private fun MonthlyProductRankModel.toRankedProduct(): RankedProduct =
        RankedProduct(rank = this.rankPosition.toLong(), productId = this.productId, score = this.score)
}
