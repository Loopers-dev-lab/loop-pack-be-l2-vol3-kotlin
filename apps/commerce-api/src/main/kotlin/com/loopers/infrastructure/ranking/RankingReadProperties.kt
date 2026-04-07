package com.loopers.infrastructure.ranking

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "ranking")
class RankingReadProperties {
    var strategy: String = "daily"

    @NestedConfigurationProperty
    var slidingWindow: SlidingWindowProperties = SlidingWindowProperties()

    fun isSlidingWindow(): Boolean = strategy.lowercase() == "sliding-window"

    class SlidingWindowProperties {
        var windowDays: Int = 7
        var decayFactor: Double = 0.9
    }
}
