package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.WeeklyRank
import com.loopers.domain.ranking.WeeklyRankRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class WeeklyRankRepositoryImpl(
    private val jpa: WeeklyRankJpaRepository,
) : WeeklyRankRepository {

    override fun findLatestWeekEnd(): LocalDate? = jpa.findLatestWeekEnd()

    override fun findRanksByWeekEnd(weekEnd: LocalDate): List<WeeklyRank> =
        jpa.findAllByIdWeekEndOrderByRankPositionAsc(weekEnd)

    override fun save(weeklyRank: WeeklyRank): WeeklyRank = jpa.save(weeklyRank)
}
