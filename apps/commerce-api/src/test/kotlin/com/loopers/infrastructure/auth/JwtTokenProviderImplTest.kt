package com.loopers.infrastructure.auth

import com.loopers.support.config.JwtConfig
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Date

@DisplayName("JwtTokenProviderImpl")
class JwtTokenProviderImplTest {

    companion object {
        private const val SECRET = "test-secret-key-for-hs256-algorithm-min-32-chars"
        private const val EXPIRATION = 3600L
        private const val ISSUER = "commerce-api"
        private const val MEMBER_ID = 123L
        private const val LOGIN_ID = "test_user1"
    }

    private val jwtConfig = JwtConfig(secret = SECRET, expiration = EXPIRATION, issuer = ISSUER)
    private val jwtTokenProvider = JwtTokenProviderImpl(jwtConfig)

    @DisplayName("generateToken")
    @Nested
    inner class GenerateToken {
        @DisplayName("유효한 토큰을 생성한다")
        @Test
        fun generatesValidToken() {
            // act
            val token = jwtTokenProvider.generateToken(MEMBER_ID, LOGIN_ID)

            // assert
            assertThat(token).isNotBlank()

            val key = Keys.hmacShaKeyFor(SECRET.toByteArray())
            val claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload

            assertThat(claims.subject).isEqualTo(MEMBER_ID.toString())
            assertThat(claims["loginId"]).isEqualTo(LOGIN_ID)
            assertThat(claims.issuer).isEqualTo(ISSUER)
            assertThat(claims.expiration).isAfter(Date())
        }
    }

    @DisplayName("validateToken")
    @Nested
    inner class ValidateToken {
        @DisplayName("유효한 토큰이면 인증된 회원 정보를 반환한다")
        @Test
        fun returnsAuthenticatedMember_whenTokenIsValid() {
            // arrange
            val token = jwtTokenProvider.generateToken(MEMBER_ID, LOGIN_ID)

            // act
            val result = jwtTokenProvider.validateToken(token)

            // assert
            assertThat(result.memberId).isEqualTo(MEMBER_ID)
            assertThat(result.loginId).isEqualTo(LOGIN_ID)
        }

        @DisplayName("만료된 토큰이면 TOKEN_EXPIRED 예외가 발생한다")
        @Test
        fun throwsTokenExpiredException_whenTokenIsExpired() {
            // arrange
            val expiredConfig = JwtConfig(secret = SECRET, expiration = -1, issuer = ISSUER)
            val expiredProvider = JwtTokenProviderImpl(expiredConfig)
            val token = expiredProvider.generateToken(MEMBER_ID, LOGIN_ID)

            // act & assert
            assertThatThrownBy { jwtTokenProvider.validateToken(token) }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.TOKEN_EXPIRED)
        }

        @DisplayName("변조된 토큰이면 INVALID_TOKEN 예외가 발생한다")
        @Test
        fun throwsInvalidTokenException_whenTokenIsTampered() {
            // arrange
            val token = jwtTokenProvider.generateToken(MEMBER_ID, LOGIN_ID)
            val tamperedToken = token + "tampered"

            // act & assert
            assertThatThrownBy { jwtTokenProvider.validateToken(tamperedToken) }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_TOKEN)
        }

        @DisplayName("잘못된 형식의 토큰이면 INVALID_TOKEN 예외가 발생한다")
        @Test
        fun throwsInvalidTokenException_whenTokenIsMalformed() {
            // act & assert
            assertThatThrownBy { jwtTokenProvider.validateToken("invalid.token.format") }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_TOKEN)
        }
    }
}
