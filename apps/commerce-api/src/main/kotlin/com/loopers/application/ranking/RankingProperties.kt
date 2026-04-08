package com.loopers.application.ranking

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ranking")
data class RankingProperties(
    val weight: Weight = Weight(),
    val ttlDays: Long = 2,
) {
    data class Weight(
        val view: Double = 0.1,
        val like: Double = 0.2,
        val order: Double = 0.6,
    )
}
