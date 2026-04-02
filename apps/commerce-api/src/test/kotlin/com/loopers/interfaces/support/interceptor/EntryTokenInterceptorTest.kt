package com.loopers.interfaces.support.interceptor

import com.loopers.application.queue.QueueFallbackHandler
import com.loopers.application.queue.ValidateEntryTokenUseCase
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.queue.token.FakeEntryTokenRepository
import com.loopers.domain.queue.token.repository.EntryTokenRepository
import com.loopers.interfaces.support.ATTRIBUTE_USER_ID
import com.loopers.interfaces.support.HEADER_ENTRY_TOKEN
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class EntryTokenInterceptorTest {

    private lateinit var entryTokenRepository: FakeEntryTokenRepository
    private lateinit var queueFallbackHandler: QueueFallbackHandler
    private lateinit var interceptor: EntryTokenInterceptor

    @BeforeEach
    fun setUp() {
        entryTokenRepository = FakeEntryTokenRepository()
        queueFallbackHandler = QueueFallbackHandler()
        interceptor = EntryTokenInterceptor(
            ValidateEntryTokenUseCase(entryTokenRepository),
            queueFallbackHandler,
        )
    }

    @Nested
    @DisplayName("preHandle 호출 시")
    inner class PreHandle {

        @Test
        @DisplayName("GET 요청 시 토큰 검증 없이 통과한다")
        fun preHandle_getRequest_bypasses() {
            // arrange
            val request = MockHttpServletRequest("GET", "/api/v1/orders")

            // act
            val result = interceptor.preHandle(request, MockHttpServletResponse(), Any())

            // assert
            assertThat(result).isTrue()
        }

        @Test
        @DisplayName("ATTRIBUTE_USER_ID가 없는 POST 요청 시 UNAUTHORIZED 예외가 발생한다")
        fun preHandle_noUserId_throwsUnauthorized() {
            // arrange
            val request = MockHttpServletRequest("POST", "/api/v1/orders")

            // act
            val exception = assertThrows<CoreException> {
                interceptor.preHandle(request, MockHttpServletResponse(), Any())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }

        @Test
        @DisplayName("X-Entry-Token 헤더가 없으면 FORBIDDEN 예외가 발생한다")
        fun preHandle_noTokenHeader_throwsForbidden() {
            // arrange
            val request = MockHttpServletRequest("POST", "/api/v1/orders")
            request.setAttribute(ATTRIBUTE_USER_ID, 1L)

            // act
            val exception = assertThrows<CoreException> {
                interceptor.preHandle(request, MockHttpServletResponse(), Any())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.FORBIDDEN)
        }

        @Test
        @DisplayName("토큰이 만료되어 Redis에 없으면 FORBIDDEN 예외가 발생한다")
        fun preHandle_tokenExpired_throwsForbidden() {
            // arrange
            val request = MockHttpServletRequest("POST", "/api/v1/orders")
            request.setAttribute(ATTRIBUTE_USER_ID, 1L)
            request.addHeader(HEADER_ENTRY_TOKEN, "expired-token")

            // act
            val exception = assertThrows<CoreException> {
                interceptor.preHandle(request, MockHttpServletResponse(), Any())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.FORBIDDEN)
        }

        @Test
        @DisplayName("헤더 토큰과 저장된 토큰이 불일치하면 FORBIDDEN 예외가 발생한다")
        fun preHandle_tokenMismatch_throwsForbidden() {
            // arrange
            val request = MockHttpServletRequest("POST", "/api/v1/orders")
            request.setAttribute(ATTRIBUTE_USER_ID, 1L)
            request.addHeader(HEADER_ENTRY_TOKEN, "wrong-token")
            entryTokenRepository.issue(UserId(1L), "correct-token", 300)

            // act
            val exception = assertThrows<CoreException> {
                interceptor.preHandle(request, MockHttpServletResponse(), Any())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.FORBIDDEN)
        }

        @Test
        @DisplayName("유효한 토큰이면 true를 반환한다")
        fun preHandle_validToken_returnsTrue() {
            // arrange
            val token = "valid-token"
            val request = MockHttpServletRequest("POST", "/api/v1/orders")
            request.setAttribute(ATTRIBUTE_USER_ID, 1L)
            request.addHeader(HEADER_ENTRY_TOKEN, token)
            entryTokenRepository.issue(UserId(1L), token, 300)

            // act
            val result = interceptor.preHandle(request, MockHttpServletResponse(), Any())

            // assert
            assertThat(result).isTrue()
        }
    }

    @Nested
    @DisplayName("Redis 장애 Fallback 시")
    inner class RedisFallback {

        @Test
        @DisplayName("Redis 연결 실패 시 503을 반환하고 fallback 상태로 전환한다")
        fun preHandle_redisFailure_throwsServiceUnavailable() {
            // arrange
            val failingRepo = object : EntryTokenRepository {
                override fun find(userId: UserId): String? =
                    throw RuntimeException("Redis connection refused")

                override fun issue(userId: UserId, token: String, ttlSeconds: Long) = Unit
                override fun delete(userId: UserId) = Unit
                override fun consumeIfValid(userId: UserId, token: String) =
                    throw RuntimeException("Redis connection refused")
            }
            val failingInterceptor = EntryTokenInterceptor(
                ValidateEntryTokenUseCase(failingRepo),
                queueFallbackHandler,
            )
            val request = MockHttpServletRequest("POST", "/api/v1/orders")
            request.setAttribute(ATTRIBUTE_USER_ID, 1L)
            request.addHeader(HEADER_ENTRY_TOKEN, "some-token")

            // act
            val exception = assertThrows<CoreException> {
                failingInterceptor.preHandle(request, MockHttpServletResponse(), Any())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.SERVICE_UNAVAILABLE)
            assertThat(queueFallbackHandler.isAvailable()).isFalse()
        }

        @Test
        @DisplayName("Redis 장애(fallback) 상태에서 503을 반환한다")
        fun preHandle_unavailable_throwsServiceUnavailable() {
            // arrange
            queueFallbackHandler.markUnavailable("Redis down")
            val request = MockHttpServletRequest("POST", "/api/v1/orders")
            request.setAttribute(ATTRIBUTE_USER_ID, 1L)

            // act
            val exception = assertThrows<CoreException> {
                interceptor.preHandle(request, MockHttpServletResponse(), Any())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.SERVICE_UNAVAILABLE)
        }

        @Test
        @DisplayName("Redis 복구 후 정상 요청이 성공한다")
        fun preHandle_afterRecovery_succeeds() {
            // arrange — 장애 후 복구 (스케줄러에 의해)
            queueFallbackHandler.markUnavailable("Redis down")
            queueFallbackHandler.markAvailable()
            val token = "valid-token"
            entryTokenRepository.issue(UserId(1L), token, 300)
            val request = MockHttpServletRequest("POST", "/api/v1/orders")
            request.setAttribute(ATTRIBUTE_USER_ID, 1L)
            request.addHeader(HEADER_ENTRY_TOKEN, token)

            // act
            val result = interceptor.preHandle(request, MockHttpServletResponse(), Any())

            // assert
            assertThat(result).isTrue()
        }

        @Test
        @DisplayName("Fail-Fast 상태에서는 토큰이 있어도 503을 반환한다")
        fun preHandle_failFast_throwsServiceUnavailable() {
            // arrange
            queueFallbackHandler.markUnavailable("Redis down")
            val request = MockHttpServletRequest("POST", "/api/v1/orders")
            request.setAttribute(ATTRIBUTE_USER_ID, 1L)
            request.addHeader(HEADER_ENTRY_TOKEN, "some-token")

            // act
            val exception = assertThrows<CoreException> {
                interceptor.preHandle(request, MockHttpServletResponse(), Any())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.SERVICE_UNAVAILABLE)
        }
    }
}
