package com.loopers.infrastructure.payment

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration
class PgClientConfig(
    @Value("\${payment.pg.base-url}") private val baseUrl: String,
    @Value("\${payment.pg.connect-timeout}") private val connectTimeout: Int,
    @Value("\${payment.pg.read-timeout}") private val readTimeout: Int,
) {

    @Bean
    fun pgRestClient(): RestClient {
        val factory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(connectTimeout)
            setReadTimeout(readTimeout)
        }

        return RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(factory)
            .build()
    }
}
