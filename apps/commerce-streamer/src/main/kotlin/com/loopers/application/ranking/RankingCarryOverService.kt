package com.loopers.application.ranking

import com.loopers.infrastructure.ranking.RankingRedisRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class RankingCarryOverService(
    private val rankingRedisRepository: RankingRedisRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun carryOver(today: LocalDate) {
        val tomorrow = today.plusDays(1)
        log.info("Score Carry-Over 실행: {} → {} (weight={})", today, tomorrow, CARRY_OVER_WEIGHT)
        rankingRedisRepository.carryOverScores(today, tomorrow, CARRY_OVER_WEIGHT)
    }

    companion object {
        const val CARRY_OVER_WEIGHT = 0.1
    }
}
