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
        @DisplayName("X-Entry-Token 헤더가 없으면 FORBIDDEN 예외가 발생한다")
        fun preHandle_noTokenHeader_throwsForbidden() {
            // arrange
            val request = MockHttpServletRequest()
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
            val request = MockHttpServletRequest()
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
            val request = MockHttpServletRequest()
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
            val request = MockHttpServletRequest()
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
        @DisplayName("Redis 연결 실패 시 토큰 검증을 건너뛰고 요청을 허용한다")
        fun preHandle_redisFailure_bypassesValidation() {
            // arrange
            val failingRepo = object : EntryTokenRepository {
                override fun find(userId: UserId): String? =
                    throw RuntimeException("Redis connection refused")

                override fun issue(userId: UserId, token: String, ttlSeconds: Long) = Unit
                override fun delete(userId: UserId) = Unit
            }
            val failingInterceptor = EntryTokenInterceptor(
                ValidateEntryTokenUseCase(failingRepo),
                queueFallbackHandler,
            )
            val request = MockHttpServletRequest()
            request.setAttribute(ATTRIBUTE_USER_ID, 1L)
            request.addHeader(HEADER_ENTRY_TOKEN, "some-token")

            // act
            val result = failingInterceptor.preHandle(request, MockHttpServletResponse(), Any())

            // assert
            assertThat(result).isTrue()
            assertThat(queueFallbackHandler.isAvailable()).isFalse()
        }

        @Test
        @DisplayName("Redis 장애 상태에서 토큰 헤더 없이도 요청을 허용한다")
        fun preHandle_unavailableAndNoToken_bypasses() {
            // arrange
            queueFallbackHandler.markUnavailable("Redis down")
            val request = MockHttpServletRequest()
            request.setAttribute(ATTRIBUTE_USER_ID, 1L)

            // act
            val result = interceptor.preHandle(request, MockHttpServletResponse(), Any())

            // assert
            assertThat(result).isTrue()
        }

        @Test
        @DisplayName("Redis 복구 시 정상 모드로 전환한다")
        fun preHandle_redisRecovery_marksAvailable() {
            // arrange
            queueFallbackHandler.markUnavailable("Redis down")
            val token = "valid-token"
            entryTokenRepository.issue(UserId(1L), token, 300)
            val request = MockHttpServletRequest()
            request.setAttribute(ATTRIBUTE_USER_ID, 1L)
            request.addHeader(HEADER_ENTRY_TOKEN, token)

            // act
            interceptor.preHandle(request, MockHttpServletResponse(), Any())

            // assert
            assertThat(queueFallbackHandler.isAvailable()).isTrue()
        }

        @Test
        @DisplayName("Redis 장애 중에도 비즈니스 예외는 그대로 전파한다")
        fun preHandle_coreException_stillThrown() {
            // arrange
            val request = MockHttpServletRequest()
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
    }
}
