package com.loopers.application.ranking

import com.loopers.domain.ranking.RankingService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Profile("!test")
@Component
class RankingCarryOverScheduler(
    private val rankingService: RankingService,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val CARRY_OVER_WEIGHT = 0.1
    }

    @Scheduled(cron = "0 50 23 * * *")
    fun carryOver() {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        log.info("랭킹 Carry-Over 시작: {} → {}", today, tomorrow)
        rankingService.carryOver(today, tomorrow, CARRY_OVER_WEIGHT)
        log.info("랭킹 Carry-Over 완료: {} → {}", today, tomorrow)
    }
}
