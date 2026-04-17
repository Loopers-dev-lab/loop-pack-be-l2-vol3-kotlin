package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.MonthlyRankingRepository
import com.loopers.domain.ranking.RankingEntry
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class JpaMonthlyRankingRepository(
    private val jpaRepository: ProductRankMonthlyJpaRepository,
) : MonthlyRankingRepository {

    override fun findRankings(rankingDate: LocalDate, offset: Long, size: Long): List<RankingEntry> {
        val page = (offset / size).toInt()
        val pageable = PageRequest.of(page, size.toInt())
        return jpaRepository.findByRankingDateOrderByRankingAsc(rankingDate, pageable)
            .map { entity ->
                RankingEntry(
                    productId = entity.productId,
                    score = entity.score,
                    rank = (entity.ranking - 1).toLong(),
                )
            }
    }

    override fun countByRankingDate(rankingDate: LocalDate): Long {
        return jpaRepository.countByRankingDate(rankingDate)
    }
}
