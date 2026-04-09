package com.loopers.config.rank

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "product.rank")
data class ProductRankProperties(
    val weight: Weight = Weight(),
    val ttlDays: Long = 2,
) {
    data class Weight(
        val view: Double = 0.1,
        val like: Double = 0.2,
        val order: Double = 0.7,
    )
}
