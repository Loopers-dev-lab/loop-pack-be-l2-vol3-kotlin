package com.loopers.infrastructure.pg

import feign.Request
import feign.Retryer
import org.springframework.context.annotation.Bean
import java.util.concurrent.TimeUnit

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

    @Bean
    fun feignRetryer(): Retryer {
        return Retryer.NEVER_RETRY
    }
}
