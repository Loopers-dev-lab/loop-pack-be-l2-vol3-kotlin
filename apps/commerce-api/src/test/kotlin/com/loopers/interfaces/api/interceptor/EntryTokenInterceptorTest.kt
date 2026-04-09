package com.loopers.interfaces.api.interceptor

import com.loopers.application.user.auth.UserAuthenticateUseCase
import com.loopers.application.user.queue.EntryTokenValidationUseCase
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.kotlin.mock

@DisplayName("EntryTokenInterceptor")
class EntryTokenInterceptorTest {
    private val userAuthenticateUseCase: UserAuthenticateUseCase = mock()
    private val entryTokenValidationUseCase: EntryTokenValidationUseCase = mock()
    private val interceptor = EntryTokenInterceptor(userAuthenticateUseCase, entryTokenValidationUseCase)
    private val request: HttpServletRequest = mock()
    private val response: HttpServletResponse = mock()
    private val handler: Any = Object()

    @Nested
    @DisplayName("유효한 토큰이면 주문 API 진입을 허용한다")
    inner class ValidToken {
        @Test
        @DisplayName("올바른 토큰이면 true를 반환한다 (소비하지 않음)")
        fun preHandle_validToken() {
            given(request.method).willReturn("POST")
            given(request.getHeader("X-Loopers-LoginId")).willReturn("testuser1")
            given(request.getHeader("X-Loopers-LoginPw")).willReturn("Password1!")
            given(request.getHeader("X-Entry-Token")).willReturn("valid-token")
            given(userAuthenticateUseCase.authenticateAndGetId("testuser1", "Password1!")).willReturn(1L)
            given(entryTokenValidationUseCase.validate(1L, "valid-token")).willReturn(true)

            val result = interceptor.preHandle(request, response, handler)

            assertThat(result).isTrue()
            then(entryTokenValidationUseCase).should().validate(1L, "valid-token")
        }
    }

    @Nested
    @DisplayName("토큰이 없거나 무효하면 진입을 거부한다")
    inner class InvalidToken {
        @Test
        @DisplayName("X-Entry-Token 헤더 누락 시 ENTRY_TOKEN_REQUIRED 예외")
        fun preHandle_noTokenHeader() {
            given(request.method).willReturn("POST")
            given(request.getHeader("X-Loopers-LoginId")).willReturn("testuser1")
            given(request.getHeader("X-Loopers-LoginPw")).willReturn("Password1!")
            given(request.getHeader("X-Entry-Token")).willReturn(null)
            given(userAuthenticateUseCase.authenticateAndGetId("testuser1", "Password1!")).willReturn(1L)

            val exception = assertThrows<CoreException> {
                interceptor.preHandle(request, response, handler)
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.ENTRY_TOKEN_REQUIRED)
        }

        @Test
        @DisplayName("잘못된 토큰이면 ENTRY_TOKEN_INVALID 예외")
        fun preHandle_invalidToken() {
            given(request.method).willReturn("POST")
            given(request.getHeader("X-Loopers-LoginId")).willReturn("testuser1")
            given(request.getHeader("X-Loopers-LoginPw")).willReturn("Password1!")
            given(request.getHeader("X-Entry-Token")).willReturn("wrong-token")
            given(userAuthenticateUseCase.authenticateAndGetId("testuser1", "Password1!")).willReturn(1L)
            given(entryTokenValidationUseCase.validate(1L, "wrong-token")).willReturn(false)

            val exception = assertThrows<CoreException> {
                interceptor.preHandle(request, response, handler)
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.ENTRY_TOKEN_INVALID)
        }

        @Test
        @DisplayName("X-Loopers-LoginId 헤더 누락 시 ENTRY_TOKEN_REQUIRED 예외")
        fun preHandle_noLoginIdHeader() {
            given(request.method).willReturn("POST")
            given(request.getHeader("X-Loopers-LoginId")).willReturn(null)

            val exception = assertThrows<CoreException> {
                interceptor.preHandle(request, response, handler)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.ENTRY_TOKEN_REQUIRED)
        }

        @Test
        @DisplayName("X-Loopers-LoginPw 헤더 누락 시 ENTRY_TOKEN_REQUIRED 예외")
        fun preHandle_noPasswordHeader() {
            given(request.method).willReturn("POST")
            given(request.getHeader("X-Loopers-LoginId")).willReturn("testuser1")
            given(request.getHeader("X-Loopers-LoginPw")).willReturn(null)

            val exception = assertThrows<CoreException> {
                interceptor.preHandle(request, response, handler)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.ENTRY_TOKEN_REQUIRED)
        }

        @Test
        @DisplayName("인증 실패 시 UNAUTHORIZED 예외가 전파된다")
        fun preHandle_authenticationFailed() {
            given(request.method).willReturn("POST")
            given(request.getHeader("X-Loopers-LoginId")).willReturn("testuser1")
            given(request.getHeader("X-Loopers-LoginPw")).willReturn("WrongPassword!")
            given(userAuthenticateUseCase.authenticateAndGetId("testuser1", "WrongPassword!"))
                .willThrow(CoreException(ErrorType.UNAUTHORIZED))

            val exception = assertThrows<CoreException> {
                interceptor.preHandle(request, response, handler)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }
    }

    @Nested
    @DisplayName("GET 요청은 토큰 검증 없이 통과한다")
    inner class GetRequest {
        @Test
        @DisplayName("GET 메서드는 true를 반환한다")
        fun preHandle_getMethod() {
            given(request.method).willReturn("GET")

            val result = interceptor.preHandle(request, response, handler)

            assertThat(result).isTrue()
        }
    }
}
