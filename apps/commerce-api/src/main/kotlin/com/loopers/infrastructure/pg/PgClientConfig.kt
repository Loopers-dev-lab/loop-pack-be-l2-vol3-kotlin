package com.loopers.infrastructure.pg

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.domain.pg.PgCommunicationLogRepository
import feign.Client
import feign.Request
import feign.Retryer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
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

    @Bean
    fun feignRetryer(): Retryer {
        return Retryer.NEVER_RETRY
    }

    @Bean
    fun pgFeignClient(
        pgCommunicationLogRepository: PgCommunicationLogRepository,
        transactionManager: PlatformTransactionManager,
        objectMapper: ObjectMapper,
    ): Client {
        return PgLoggingClient(
            delegate = Client.Default(null, null),
            pgCommunicationLogRepository = pgCommunicationLogRepository,
            transactionManager = transactionManager,
            objectMapper = objectMapper,
        )
    }
}
