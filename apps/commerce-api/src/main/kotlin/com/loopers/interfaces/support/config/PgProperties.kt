package com.loopers.interfaces.support.config

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "pg")
data class PgProperties(
    @field:NotBlank
    val baseUrl: String,
    @field:NotBlank
    val callbackUrl: String,
)
