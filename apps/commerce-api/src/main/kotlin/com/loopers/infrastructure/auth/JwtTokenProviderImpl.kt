package com.loopers.infrastructure.auth

import com.loopers.domain.auth.AuthenticatedMember
import com.loopers.domain.auth.JwtTokenProvider
import com.loopers.support.config.JwtConfig
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenProviderImpl(
    private val jwtConfig: JwtConfig,
) : JwtTokenProvider {

    private val secretKey: SecretKey = Keys.hmacShaKeyFor(jwtConfig.secret.toByteArray())

    override fun generateToken(memberId: Long, loginId: String): String {
        val now = Date()
        val expiration = Date(now.time + jwtConfig.expiration * 1000)

        return Jwts.builder()
            .subject(memberId.toString())
            .claim("loginId", loginId)
            .issuer(jwtConfig.issuer)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(secretKey)
            .compact()
    }

    override fun validateToken(token: String): AuthenticatedMember {
        try {
            val claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload

            return AuthenticatedMember(
                memberId = claims.subject.toLong(),
                loginId = claims["loginId", String::class.java],
            )
        } catch (e: ExpiredJwtException) {
            throw CoreException(ErrorType.TOKEN_EXPIRED)
        } catch (e: JwtException) {
            throw CoreException(ErrorType.INVALID_TOKEN)
        }
    }
}
