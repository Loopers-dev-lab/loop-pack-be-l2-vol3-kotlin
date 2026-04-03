package com.loopers.interfaces.api.queue

import com.loopers.application.queue.QueueProperties
import com.loopers.application.queue.VerifyEntryTokenUseCase
import com.loopers.application.user.AuthenticateUserUseCase
import com.loopers.support.constant.AuthHeaders
import com.loopers.support.constant.RequestAttributes
import com.loopers.support.error.CoreException
import com.loopers.support.error.QueueErrorCode
import com.loopers.support.error.UserErrorCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class EntryTokenInterceptorTest {

    private val authenticateUserUseCase: AuthenticateUserUseCase = mockk()
    private val verifyEntryTokenUseCase: VerifyEntryTokenUseCase = mockk()
    private val queueProperties = QueueProperties(
        scheduler = QueueProperties.Scheduler(intervalMs = 100, batchSize = 18),
        token = QueueProperties.Token(ttlSeconds = 300),
        redis = QueueProperties.RedisKeys(),
        maxQueueSize = 50000,
        enabled = true,
    )
    private val interceptor = EntryTokenInterceptor(authenticateUserUseCase, verifyEntryTokenUseCase, queueProperties)

    private fun createRequest(method: String = "POST"): MockHttpServletRequest {
        val request = MockHttpServletRequest()
        request.method = method
        request.addHeader(AuthHeaders.User.LOGIN_ID, "testuser")
        request.addHeader(AuthHeaders.User.LOGIN_PW, "password123")
        return request
    }

    @DisplayName("토큰 검증 인터셉터")
    @Nested
    inner class PreHandle {

        @DisplayName("토큰이 있으면 요청을 통과시키고 userId를 request attribute에 저장한다")
        @Test
        fun passWithToken() {
            every { authenticateUserUseCase.execute("testuser", "password123") } returns 1L
            every { verifyEntryTokenUseCase.execute(1L) } returns true
            val request = createRequest()

            val result = interceptor.preHandle(request, MockHttpServletResponse(), Any())

            assertThat(result).isTrue()
            assertThat(request.getAttribute(RequestAttributes.RESOLVED_USER_ID)).isEqualTo(1L)
        }

        @DisplayName("토큰이 없으면 ENTRY_TOKEN_REQUIRED 예외가 발생한다")
        @Test
        fun rejectWithoutToken() {
            every { authenticateUserUseCase.execute("testuser", "password123") } returns 1L
            every { verifyEntryTokenUseCase.execute(1L) } returns false

            val exception = assertThrows<CoreException> {
                interceptor.preHandle(createRequest(), MockHttpServletResponse(), Any())
            }
            assertThat(exception.errorCode).isEqualTo(QueueErrorCode.ENTRY_TOKEN_REQUIRED)
        }

        @DisplayName("GET 요청은 토큰 검증 없이 통과한다")
        @Test
        fun passGetRequest() {
            val request = createRequest(method = "GET")

            val result = interceptor.preHandle(request, MockHttpServletResponse(), Any())

            assertThat(result).isTrue()
            verify(exactly = 0) { verifyEntryTokenUseCase.execute(any()) }
        }

        @DisplayName("인증 헤더가 없으면 토큰 검증 없이 통과한다")
        @Test
        fun passWithoutAuthHeaders() {
            val request = MockHttpServletRequest()
            request.method = "POST"

            val result = interceptor.preHandle(request, MockHttpServletResponse(), Any())

            assertThat(result).isTrue()
            verify(exactly = 0) { authenticateUserUseCase.execute(any(), any()) }
        }

        @DisplayName("인증 실패 시 토큰 검증 없이 통과한다 (ArgumentResolver에서 처리)")
        @Test
        fun passOnAuthFailure() {
            every { authenticateUserUseCase.execute("testuser", "password123") } throws
                CoreException(UserErrorCode.AUTHENTICATION_FAILED)

            val result = interceptor.preHandle(createRequest(), MockHttpServletResponse(), Any())

            assertThat(result).isTrue()
            verify(exactly = 0) { verifyEntryTokenUseCase.execute(any()) }
        }

        @DisplayName("Redis 장애 시 fail-open으로 통과시킨다")
        @Test
        fun failOpenOnRedisFailure() {
            every { authenticateUserUseCase.execute("testuser", "password123") } returns 1L
            every { verifyEntryTokenUseCase.execute(1L) } throws RedisConnectionFailureException("Connection refused")
            val request = createRequest()

            val result = interceptor.preHandle(request, MockHttpServletResponse(), Any())

            assertThat(result).isTrue()
            assertThat(request.getAttribute(RequestAttributes.RESOLVED_USER_ID)).isEqualTo(1L)
        }

        @DisplayName("enabled가 false이면 토큰 검증 없이 통과한다")
        @Test
        fun passWhenDisabled() {
            val disabledProperties = queueProperties.copy(enabled = false)
            val disabledInterceptor = EntryTokenInterceptor(authenticateUserUseCase, verifyEntryTokenUseCase, disabledProperties)

            val result = disabledInterceptor.preHandle(createRequest(), MockHttpServletResponse(), Any())

            assertThat(result).isTrue()
            verify(exactly = 0) { authenticateUserUseCase.execute(any(), any()) }
        }
    }
}
