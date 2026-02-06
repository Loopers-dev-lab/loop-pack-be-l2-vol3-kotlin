package com.loopers.application.auth

data class TokenInfo(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long,
)
