package com.loopers.config

import com.loopers.config.redis.RedisRankingConstants
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class TimeConfig {
    @Bean
    fun clock(): Clock = Clock.system(RedisRankingConstants.KST_ZONE)
}
