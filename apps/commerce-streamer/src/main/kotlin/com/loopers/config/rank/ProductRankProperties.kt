package com.loopers.config.rank

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "product.rank")
data class ProductRankProperties(
    val weight: Weight = Weight(),
    val ttlDays: Long = 2,
) {
    init {
        require(ttlDays >= 1) {
            "product.rank.ttl-days는 1 이상이어야 합니다: $ttlDays"
        }
    }

    data class Weight(
        val view: Double = 0.1,
        val like: Double = 0.2,
        val order: Double = 0.7,
    ) {
        init {
            require(view >= 0.0) { "product.rank.weight.view는 0 이상이어야 합니다: $view" }
            require(like >= 0.0) { "product.rank.weight.like는 0 이상이어야 합니다: $like" }
            require(order >= 0.0) { "product.rank.weight.order는 0 이상이어야 합니다: $order" }
        }
    }
}
