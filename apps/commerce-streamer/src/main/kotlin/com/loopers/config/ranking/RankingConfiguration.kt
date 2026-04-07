package com.loopers.config.ranking

import com.loopers.domain.ranking.DailyAccumulationStrategy
import com.loopers.domain.ranking.RankingScoreStrategy
import com.loopers.domain.ranking.SlidingWindowStrategy
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(RankingProperties::class)
class RankingConfiguration {

    @Bean
    fun rankingScoreStrategy(properties: RankingProperties): RankingScoreStrategy {
        return when (properties.strategy.lowercase()) {
            "sliding-window" -> SlidingWindowStrategy(
                windowDays = properties.slidingWindow.windowDays,
                decayFactor = properties.slidingWindow.decayFactor,
            )
            "daily" -> DailyAccumulationStrategy()
            else -> throw IllegalArgumentException(
                "Unknown ranking strategy: ${properties.strategy}. " +
                    "Supported values: 'daily', 'sliding-window'",
            )
        }
    }
}
