package com.loopers.domain.ranking

import java.time.LocalDate

interface WeeklyRankRepository {

    fun findLatestWeekEnd(): LocalDate?

    fun findRanksByWeekEnd(weekEnd: LocalDate): List<WeeklyRank>

    fun save(weeklyRank: WeeklyRank): WeeklyRank
}
