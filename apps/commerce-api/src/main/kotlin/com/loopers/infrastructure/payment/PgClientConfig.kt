package com.loopers.infrastructure.payment

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.time.Duration

@ConfigurationProperties(prefix = "pg")
data class PgProperties(
    val baseUrl: String,
    val callbackUrl: String,
    val connectTimeout: Long,
    val readTimeout: Long,
)

@Configuration
class PgClientConfig {
    @Suppress("DEPRECATION")
    @Bean
    fun pgRestTemplate(
        pgProperties: PgProperties,
        builder: RestTemplateBuilder,
    ): RestTemplate {
        return builder
            .rootUri(pgProperties.baseUrl)
            .setConnectTimeout(Duration.ofMillis(pgProperties.connectTimeout))
            .setReadTimeout(Duration.ofMillis(pgProperties.readTimeout))
            .build()
    }
}
