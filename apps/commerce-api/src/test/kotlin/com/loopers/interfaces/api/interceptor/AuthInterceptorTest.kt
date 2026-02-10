package com.loopers.interfaces.api.interceptor

import com.loopers.application.auth.AuthService
import com.loopers.domain.user.User
import com.loopers.interfaces.api.ATTRIBUTE_USER_ID
import com.loopers.interfaces.api.HEADER_LOGIN_ID
import com.loopers.interfaces.api.HEADER_LOGIN_PW
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class AuthInterceptorTest {

    private lateinit var authService: AuthService
    private lateinit var authInterceptor: AuthInterceptor

    @BeforeEach
    fun setUp() {
        authService = mockk()
        authInterceptor = AuthInterceptor(authService)
    }

    @Nested
    @DisplayName("preHandle 호출 시")
    inner class PreHandle {

        @Test
        @DisplayName("인증 성공 시 request에 userId를 설정하고 true를 반환한다")
        fun preHandle_withValidCredentials_setsUserIdAndReturnsTrue() {
            // arrange
            val request = MockHttpServletRequest()
            request.addHeader(HEADER_LOGIN_ID, "testuser")
            request.addHeader(HEADER_LOGIN_PW, "Password1!")

            val user = mockk<User>()
            every { user.id } returns 1L
            every { authService.authenticate("testuser", "Password1!") } returns user

            // act
            val result = authInterceptor.preHandle(request, MockHttpServletResponse(), Any())

            // assert
            assertThat(result).isTrue()
            assertThat(request.getAttribute(ATTRIBUTE_USER_ID)).isEqualTo(1L)
        }

        @Test
        @DisplayName("로그인 ID 헤더가 없으면 BAD_REQUEST 예외가 발생한다")
        fun preHandle_withoutLoginIdHeader_throwsBadRequest() {
            // arrange
            val request = MockHttpServletRequest()
            request.addHeader(HEADER_LOGIN_PW, "Password1!")

            // act
            val exception = assertThrows<CoreException> {
                authInterceptor.preHandle(request, MockHttpServletResponse(), Any())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("비밀번호 헤더가 없으면 BAD_REQUEST 예외가 발생한다")
        fun preHandle_withoutLoginPwHeader_throwsBadRequest() {
            // arrange
            val request = MockHttpServletRequest()
            request.addHeader(HEADER_LOGIN_ID, "testuser")

            // act
            val exception = assertThrows<CoreException> {
                authInterceptor.preHandle(request, MockHttpServletResponse(), Any())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
