package com.loopers.config

import com.loopers.event.EventContract
import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "ranking")
data class RankingProperties(
    val weights: Weights = Weights(),
    val ttl: Ttl = Ttl(),
    val keyPrefix: String = EventContract.RANKING_KEY_PREFIX,
) {
    data class Weights(
        val view: Double = 0.1,
        val like: Double = 0.2,
        val order: Double = 0.7,
    )

    data class Ttl(
        val daily: Duration = Duration.ofDays(2),
        val hourly: Duration = Duration.ofHours(3),
    )
}
