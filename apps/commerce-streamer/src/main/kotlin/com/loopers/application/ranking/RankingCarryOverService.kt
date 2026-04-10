package com.loopers.application.ranking

import com.loopers.infrastructure.ranking.RankingRedisRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class RankingCarryOverService(
    private val rankingRedisRepository: RankingRedisRepository,
    private val rankingWeightProvider: RankingWeightProvider,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun carryOver(today: LocalDate) {
        val tomorrow = today.plusDays(1)
        val weight = rankingWeightProvider.getCarryOverWeight()
        log.info("Score Carry-Over 실행: {} → {} (weight={})", today, tomorrow, weight)
        rankingRedisRepository.carryOverScores(today, tomorrow, weight)
    }
}
