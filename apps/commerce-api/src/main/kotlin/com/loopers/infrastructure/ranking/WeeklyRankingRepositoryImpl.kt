package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.model.WeeklyProductRank
import com.loopers.domain.ranking.repository.WeeklyRankingRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface WeeklyRankingJpaRepository : JpaRepository<MvProductRankWeeklyEntity, Long> {
    fun findTop100ByPeriodKeyAndDeletedAtIsNullOrderByRankNoAsc(periodKey: String): List<MvProductRankWeeklyEntity>
}

@Repository
class WeeklyRankingRepositoryImpl(
    private val weeklyRankingJpaRepository: WeeklyRankingJpaRepository,
) : WeeklyRankingRepository {

    override fun findAllByPeriodKey(periodKey: String): List<WeeklyProductRank> {
        return weeklyRankingJpaRepository.findTop100ByPeriodKeyAndDeletedAtIsNullOrderByRankNoAsc(periodKey)
            .map { it.toDomain() }
    }
}
