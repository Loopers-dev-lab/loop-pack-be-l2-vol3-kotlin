package com.loopers.infrastructure.payment

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.ClientHttpRequestFactories
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

@Configuration
class PgClientConfig(
    @Value("\${pg.base-url}") private val baseUrl: String,
    @Value("\${pg.connect-timeout-ms:200}") private val connectTimeoutMs: Long,
) {

    @Bean
    fun pgRestClient(): RestClient {
        val settings = ClientHttpRequestFactorySettings.DEFAULTS
            .withConnectTimeout(Duration.ofMillis(connectTimeoutMs))

        return RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(ClientHttpRequestFactories.get(settings))
            .build()
    }

    @Bean
    fun pgOutboundExecutor(): ExecutorService {
        return Executors.newFixedThreadPool(10)
    }

    @Bean
    fun recoveryScheduler(): ScheduledExecutorService {
        return Executors.newScheduledThreadPool(2)
    }
}
