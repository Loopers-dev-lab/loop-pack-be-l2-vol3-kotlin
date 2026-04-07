package com.loopers.config.ranking

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

@ConfigurationProperties(prefix = "ranking")
class RankingProperties {
    var scoring: String = "default"
    var aggregation: String = "daily"

    @NestedConfigurationProperty
    var slidingWindow: SlidingWindowProperties = SlidingWindowProperties()

    class SlidingWindowProperties {
        var windowDays: Int = 7
        var decayFactor: Double = 0.9
    }
}
