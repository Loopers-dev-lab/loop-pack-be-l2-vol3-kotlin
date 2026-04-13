package com.loopers.config.ranking

import com.loopers.domain.ranking.DefaultScoringStrategy
import com.loopers.domain.ranking.ScoringStrategy
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(RankingProperties::class)
class RankingConfiguration {

    @Bean
    fun scoringStrategy(properties: RankingProperties): ScoringStrategy {
        return when (properties.scoring.lowercase()) {
            "default" -> DefaultScoringStrategy()
            else -> throw IllegalArgumentException(
                "Unknown scoring strategy: ${properties.scoring}. Supported values: 'default'",
            )
        }
    }
}
