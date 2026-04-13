package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.ProductRankingRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId

@Component
class RankingCarryOverScheduler(
    private val productRankingRepository: ProductRankingRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 50 23 * * *", zone = "Asia/Seoul")
    fun execute() {
        runCarryOver()
    }

    fun runCarryOver() {
        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
        val tomorrow = today.plusDays(1)

        runCatching {
            if (!productRankingRepository.exists(today)) {
                log.info("Skip carry-over. No ranking data for today={}.", today)
                return
            }
            productRankingRepository.carryOver(today, tomorrow, CARRY_OVER_WEIGHT)
            log.info("Carry-over completed. source={}, destination={}, weight={}", today, tomorrow, CARRY_OVER_WEIGHT)
        }.onFailure { e ->
            log.warn("Failed to execute carry-over. today={}", today, e)
        }
    }

    companion object {
        const val CARRY_OVER_WEIGHT = 0.1
    }
}
