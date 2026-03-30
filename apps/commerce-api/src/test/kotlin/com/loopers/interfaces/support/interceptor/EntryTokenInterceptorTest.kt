package com.loopers.interfaces.support.interceptor

import com.loopers.application.queue.ValidateEntryTokenUseCase
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.queue.token.FakeEntryTokenRepository
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
    private lateinit var interceptor: EntryTokenInterceptor

    @BeforeEach
    fun setUp() {
        entryTokenRepository = FakeEntryTokenRepository()
        interceptor = EntryTokenInterceptor(
            ValidateEntryTokenUseCase(entryTokenRepository),
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
}
