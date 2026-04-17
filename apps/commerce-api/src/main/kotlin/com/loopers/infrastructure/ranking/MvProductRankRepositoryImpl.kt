package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.MvProductRankRepository
import com.loopers.infrastructure.catalog.RankedProductEntry
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
class MvProductRankRepositoryImpl(
    private val weeklyJpaRepository: MvProductRankWeeklyJpaRepository,
    private val monthlyJpaRepository: MvProductRankMonthlyJpaRepository,
) : MvProductRankRepository {

    override fun getWeeklyRanking(page: Int, size: Int): List<RankedProductEntry> {
        val pageable = PageRequest.of(page, size)
        val offset = page.toLong() * size
        return weeklyJpaRepository.findAllOrderByScoreDesc(pageable)
            .mapIndexed { index, model ->
                RankedProductEntry(
                    productId = model.productId,
                    score = model.score,
                    rank = offset + index,
                )
            }
    }

    override fun getWeeklyTotalCount(): Long {
        return weeklyJpaRepository.count()
    }

    override fun getMonthlyRanking(page: Int, size: Int): List<RankedProductEntry> {
        val pageable = PageRequest.of(page, size)
        val offset = page.toLong() * size
        return monthlyJpaRepository.findAllOrderByScoreDesc(pageable)
            .mapIndexed { index, model ->
                RankedProductEntry(
                    productId = model.productId,
                    score = model.score,
                    rank = offset + index,
                )
            }
    }

    override fun getMonthlyTotalCount(): Long {
        return monthlyJpaRepository.count()
    }
}
