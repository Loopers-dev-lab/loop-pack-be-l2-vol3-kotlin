package com.loopers.support.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt")
data class JwtConfig(
    val secret: String,
    val expiration: Long,
    val issuer: String,
)
