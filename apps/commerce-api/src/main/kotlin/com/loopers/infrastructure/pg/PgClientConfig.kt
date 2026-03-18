package com.loopers.infrastructure.pg

import feign.Request
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
class PgClientConfig {

    @Bean
    fun feignOptions(): Request.Options {
        return Request.Options(
            1000,
            TimeUnit.MILLISECONDS,
            3000,
            TimeUnit.MILLISECONDS,
            true,
        )
    }
}
