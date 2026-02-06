package com.loopers.application.auth

import com.loopers.domain.auth.AuthService
import com.loopers.domain.auth.JwtTokenProvider
import com.loopers.support.config.JwtConfig
import org.springframework.stereotype.Component

@Component
class AuthFacade(
    private val authService: AuthService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val jwtConfig: JwtConfig,
) {
    fun login(loginId: String, rawPassword: String): TokenInfo {
        val member = authService.authenticate(loginId, rawPassword)

        val accessToken = jwtTokenProvider.generateToken(member.id, member.loginId)

        return TokenInfo(
            accessToken = accessToken,
            tokenType = "Bearer",
            expiresIn = jwtConfig.expiration,
        )
    }
}
