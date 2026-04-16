package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.MvProductRankMonthly
import com.loopers.domain.ranking.MvProductRankRepository
import com.loopers.domain.ranking.MvProductRankWeekly
import com.loopers.domain.ranking.ProductRankingReadModel
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
class MvProductRankRepositoryImpl(
    private val weeklyJpaRepository: MvProductRankWeeklyJpaRepository,
    private val monthlyJpaRepository: MvProductRankMonthlyJpaRepository,
) : MvProductRankRepository {

    override fun findWeeklyRanking(yearWeek: String, page: Int, size: Int): List<ProductRankingReadModel> {
        val pageable = PageRequest.of(page, size)
        return weeklyJpaRepository.findByYearWeekOrderByRank(yearWeek, pageable)
            .map {
                ProductRankingReadModel(
                    productId = it.productId,
                    rank = it.rank.toLong(),
                    score = it.score,
                )
            }
            .toList()
    }

    override fun findMonthlyRanking(yearMonth: String, page: Int, size: Int): List<ProductRankingReadModel> {
        val pageable = PageRequest.of(page, size)
        return monthlyJpaRepository.findByYearMonthOrderByRank(yearMonth, pageable)
            .map {
                ProductRankingReadModel(
                    productId = it.productId,
                    rank = it.rank.toLong(),
                    score = it.score,
                )
            }
            .toList()
    }

    override fun countWeekly(yearWeek: String): Long {
        return weeklyJpaRepository.countByYearWeek(yearWeek)
    }

    override fun countMonthly(yearMonth: String): Long {
        return monthlyJpaRepository.countByYearMonth(yearMonth)
    }
}

interface MvProductRankWeeklyJpaRepository : JpaRepository<MvProductRankWeekly, Long> {
    fun findByYearWeekOrderByRank(
        yearWeek: String,
        pageable: org.springframework.data.domain.Pageable,
    ): org.springframework.data.domain.Page<MvProductRankWeekly>
    fun countByYearWeek(yearWeek: String): Long
}

interface MvProductRankMonthlyJpaRepository : JpaRepository<MvProductRankMonthly, Long> {
    fun findByYearMonthOrderByRank(
        yearMonth: String,
        pageable: org.springframework.data.domain.Pageable,
    ): org.springframework.data.domain.Page<MvProductRankMonthly>
    fun countByYearMonth(yearMonth: String): Long
}
