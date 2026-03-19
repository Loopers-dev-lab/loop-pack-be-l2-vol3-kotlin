package com.loopers.config.pg

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient

@ConfigurationProperties(prefix = "pg")
data class PGProperties(
    val baseUrl: String = "http://localhost:8082",
    val callbackUrl: String = "http://localhost:8080/api/v1/payments/callback",
    val connectTimeout: Int = 3000,
    val readTimeout: Int = 3000,
)

@Configuration
@EnableConfigurationProperties(PGProperties::class)
class PGClientConfig {

    @Bean
    fun pgRestClient(pgProperties: PGProperties): RestClient {
        val factory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(pgProperties.connectTimeout)
            setReadTimeout(pgProperties.readTimeout)
        }
        return RestClient.builder()
            .baseUrl(pgProperties.baseUrl)
            .requestFactory(factory)
            .build()
    }
}
