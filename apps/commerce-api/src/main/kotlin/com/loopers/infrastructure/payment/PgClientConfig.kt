package com.loopers.infrastructure.payment

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Configuration
class PgClientConfig(
    @Value("\${pg.base-url}") private val baseUrl: String,
    @Value("\${pg.connect-timeout-ms:200}") private val connectTimeoutMs: Long,
) {

    @Bean
    fun pgRestClient(): RestClient {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(connectTimeoutMs.toInt())
        }

        return RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .build()
    }

    @Bean
    fun pgOutboundExecutor(): ExecutorService {
        return ThreadPoolExecutor(
            10,
            10,
            0L,
            TimeUnit.MILLISECONDS,
            ArrayBlockingQueue(100),
            ThreadPoolExecutor.AbortPolicy(),
        )
    }

    @Bean
    fun recoveryScheduler(): ScheduledExecutorService {
        return Executors.newScheduledThreadPool(2)
    }
}
