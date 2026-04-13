package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.model.MonthlyProductRank
import com.loopers.domain.ranking.repository.MonthlyRankingRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface MonthlyRankingJpaRepository : JpaRepository<MvProductRankMonthlyEntity, Long> {
    fun findTop100ByPeriodKeyAndDeletedAtIsNullOrderByRankNoAsc(periodKey: String): List<MvProductRankMonthlyEntity>
}

@Repository
class MonthlyRankingRepositoryImpl(
    private val monthlyRankingJpaRepository: MonthlyRankingJpaRepository,
) : MonthlyRankingRepository {

    override fun findAllByPeriodKey(periodKey: String): List<MonthlyProductRank> {
        return monthlyRankingJpaRepository.findTop100ByPeriodKeyAndDeletedAtIsNullOrderByRankNoAsc(periodKey)
            .map { it.toDomain() }
    }
}
