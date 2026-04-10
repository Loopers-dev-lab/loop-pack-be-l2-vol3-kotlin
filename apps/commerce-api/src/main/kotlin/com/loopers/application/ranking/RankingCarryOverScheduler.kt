package com.loopers.application.ranking

import com.loopers.infrastructure.ranking.RankingRedisReader
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Component
class RankingCarryOverScheduler(
    private val rankingCarryOverProperties: RankingCarryOverProperties,
    private val rankingRedisReader: RankingRedisReader,
) {
    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    private val log = LoggerFactory.getLogger(RankingCarryOverScheduler::class.java)

    @Scheduled(
        cron = "\${ranking.carry-over.scheduler.cron:0 50 23 * * *}",
        zone = "\${ranking.carry-over.scheduler.zone:Asia/Seoul}",
    )
    fun warmUpNextDayRanking() {
        if (!rankingCarryOverProperties.enabled || !rankingCarryOverProperties.scheduler.enabled) {
            return
        }

        warmUp(baseDateTime = ZonedDateTime.now(ZoneId.of(rankingCarryOverProperties.scheduler.zone)))
    }

    fun warmUp(baseDateTime: ZonedDateTime) {
        if (!rankingCarryOverProperties.enabled || !rankingCarryOverProperties.scheduler.enabled) {
            return
        }

        val fromDate = DATE_FORMATTER.format(baseDateTime)
        val toDate = DATE_FORMATTER.format(baseDateTime.plusDays(1))
        rankingRedisReader.carryOver(
            fromDate = fromDate,
            toDate = toDate,
            limit = rankingCarryOverProperties.limit,
            multiplier = rankingCarryOverProperties.multiplier,
        )
        log.info(
            "ranking carry-over completed fromDate={} toDate={} limit={} multiplier={}",
            fromDate,
            toDate,
            rankingCarryOverProperties.limit,
            rankingCarryOverProperties.multiplier,
        )
    }
}
