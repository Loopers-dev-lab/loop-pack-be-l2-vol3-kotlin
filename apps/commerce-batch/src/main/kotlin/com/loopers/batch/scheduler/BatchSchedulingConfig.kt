package com.loopers.batch.scheduler

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.Clock
import java.time.ZoneId

@Configuration
@Profile("scheduler")
@EnableScheduling
class BatchSchedulingConfig {
    @Bean
    fun batchClock(): Clock = Clock.system(ZoneId.of("Asia/Seoul"))
}
