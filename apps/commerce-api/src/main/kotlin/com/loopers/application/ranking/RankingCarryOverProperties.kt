package com.loopers.application.ranking

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ranking.carry-over")
data class RankingCarryOverProperties(
    val enabled: Boolean = true,
    val limit: Long = 20,
    val multiplier: Double = 0.2,
    val scheduler: Scheduler = Scheduler(),
) {
    init {
        require(limit > 0) { "ranking carry-over limit must be positive" }
        require(multiplier >= 0.0) { "ranking carry-over multiplier must be non-negative" }
    }

    data class Scheduler(
        val enabled: Boolean = true,
        val cron: String = "0 50 23 * * *",
        val zone: String = "Asia/Seoul",
    )
}
