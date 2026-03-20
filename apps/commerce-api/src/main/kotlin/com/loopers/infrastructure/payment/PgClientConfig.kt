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
    val connectTimeout: Long = 2000,
    val readTimeout: Long = 3000,
    val callbackUrl: String,
)

@Configuration
class PgClientConfig {
    @Bean
    fun pgRestTemplate(pgProperties: PgProperties): RestTemplate {
        return RestTemplateBuilder()
            .rootUri(pgProperties.baseUrl)
            .connectTimeout(Duration.ofMillis(pgProperties.connectTimeout))
            .readTimeout(Duration.ofMillis(pgProperties.readTimeout))
            .build()
    }
}
