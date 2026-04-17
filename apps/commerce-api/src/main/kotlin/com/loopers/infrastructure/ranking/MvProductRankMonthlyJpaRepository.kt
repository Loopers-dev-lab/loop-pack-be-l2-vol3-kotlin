package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.MvProductRankMonthlyModel
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface MvProductRankMonthlyJpaRepository : JpaRepository<MvProductRankMonthlyModel, Long> {
    @Query("SELECT m FROM MvProductRankMonthlyModel m ORDER BY m.score DESC")
    fun findAllOrderByScoreDesc(pageable: Pageable): List<MvProductRankMonthlyModel>
}
