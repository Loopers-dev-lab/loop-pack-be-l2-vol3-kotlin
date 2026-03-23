package com.loopers.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfig {

    @Bean(name = ["eventListenerExecutor"])
    fun eventListenerExecutor(): Executor = ThreadPoolTaskExecutor().apply {
        corePoolSize = 5
        maxPoolSize = 10
        queueCapacity = 100
        threadNamePrefix = "event-listener-"
        setWaitForTasksToCompleteOnShutdown(true)
        setAwaitTerminationSeconds(60)
        initialize()
    }
}
