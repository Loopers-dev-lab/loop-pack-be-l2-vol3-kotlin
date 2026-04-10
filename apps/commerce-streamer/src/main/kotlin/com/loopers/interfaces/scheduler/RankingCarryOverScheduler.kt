package com.loopers.interfaces.scheduler

import com.loopers.application.ranking.RankingCarryOverService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class RankingCarryOverScheduler(
    private val rankingCarryOverService: RankingCarryOverService,
) {

    @Scheduled(cron = "0 50 23 * * *")
    fun executeCarryOver() {
        rankingCarryOverService.carryOver(LocalDate.now())
    }
}
