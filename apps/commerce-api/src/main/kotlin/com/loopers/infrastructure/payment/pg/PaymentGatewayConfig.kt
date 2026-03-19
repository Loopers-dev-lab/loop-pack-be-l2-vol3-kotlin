package com.loopers.infrastructure.payment.pg

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration
import java.util.concurrent.TimeUnit

@Configuration
class PaymentGatewayConfig {

    @Bean
    fun webClient(
        builder: WebClient.Builder,
        @Value("\${pg.timeout.connect-ms}") connectTimeoutMs: Int,
        @Value("\${pg.timeout.read-sec}") readTimeoutSec: Long,
        @Value("\${pg.timeout.write-sec}") writeTimeoutSec: Long,
    ): WebClient {
        val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
            .responseTimeout(Duration.ofSeconds(readTimeoutSec))
            .doOnConnected { conn ->
                conn.addHandlerLast(ReadTimeoutHandler(readTimeoutSec, TimeUnit.SECONDS))
                conn.addHandlerLast(WriteTimeoutHandler(writeTimeoutSec, TimeUnit.SECONDS))
            }

        return builder
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .build()
    }
}
