package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.MvProductRankWeeklyModel
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface MvProductRankWeeklyJpaRepository : JpaRepository<MvProductRankWeeklyModel, Long> {
    @Query("SELECT m FROM MvProductRankWeeklyModel m ORDER BY m.score DESC")
    fun findAllOrderByScoreDesc(pageable: Pageable): List<MvProductRankWeeklyModel>
}
