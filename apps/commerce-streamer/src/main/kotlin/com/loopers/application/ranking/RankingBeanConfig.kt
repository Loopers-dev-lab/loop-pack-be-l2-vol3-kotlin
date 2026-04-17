package com.loopers.application.ranking

import com.loopers.domain.ranking.RankingScorePolicy
import com.loopers.domain.ranking.ViewTrustScoreCalculator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RankingBeanConfig {

    @Bean
    fun rankingScorePolicy(): RankingScorePolicy = RankingScorePolicy()

    @Bean
    fun viewTrustScoreCalculator(): ViewTrustScoreCalculator = ViewTrustScoreCalculator()
}
