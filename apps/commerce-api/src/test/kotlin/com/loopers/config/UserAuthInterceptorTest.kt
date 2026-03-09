package com.loopers.config

import com.loopers.domain.user.UserService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

@DisplayName("UserAuthInterceptor")
class UserAuthInterceptorTest {
    companion object {
        private const val LOGIN_ID_HEADER = "X-Loopers-LoginId"
        private const val LOGIN_PW_HEADER = "X-Loopers-LoginPw"
        private const val DEFAULT_LOGIN_ID = "testuser"
        private const val DEFAULT_LOGIN_PW = "Test1234!"
    }

    private val userService: UserService = mockk()
    private val sut = UserAuthInterceptor(userService)
    private val response = MockHttpServletResponse()
    private val handler = Any()

    @DisplayName("preHandle")
    @Nested
    inner class PreHandle {
        @DisplayName("유효한 헤더와 인증 정보가 있으면, true를 반환한다.")
        @Test
        fun returnsTrueWhenValidHeadersAndCredentialsAreProvided() {
            // arrange
            val request = MockHttpServletRequest().apply {
                addHeader(LOGIN_ID_HEADER, DEFAULT_LOGIN_ID)
                addHeader(LOGIN_PW_HEADER, DEFAULT_LOGIN_PW)
            }
            every { userService.authenticate(DEFAULT_LOGIN_ID, DEFAULT_LOGIN_PW) } returns Unit

            // act
            val result = sut.preHandle(request, response, handler)

            // assert
            assertThat(result).isTrue()
            verify { userService.authenticate(DEFAULT_LOGIN_ID, DEFAULT_LOGIN_PW) }
        }

        @DisplayName("LoginId 헤더가 누락되면, UNAUTHORIZED 예외가 발생한다.")
        @Test
        fun throwsUnauthorizedWhenLoginIdHeaderIsMissing() {
            // arrange
            val request = MockHttpServletRequest().apply {
                addHeader(LOGIN_PW_HEADER, DEFAULT_LOGIN_PW)
            }

            // act & assert
            assertThatThrownBy { sut.preHandle(request, response, handler) }
                .isInstanceOf(CoreException::class.java)
                .extracting("errorType")
                .isEqualTo(ErrorType.UNAUTHORIZED)
        }

        @DisplayName("LoginPw 헤더가 누락되면, UNAUTHORIZED 예외가 발생한다.")
        @Test
        fun throwsUnauthorizedWhenLoginPwHeaderIsMissing() {
            // arrange
            val request = MockHttpServletRequest().apply {
                addHeader(LOGIN_ID_HEADER, DEFAULT_LOGIN_ID)
            }

            // act & assert
            assertThatThrownBy { sut.preHandle(request, response, handler) }
                .isInstanceOf(CoreException::class.java)
                .extracting("errorType")
                .isEqualTo(ErrorType.UNAUTHORIZED)
        }

        @DisplayName("인증에 실패하면, UserService가 UNAUTHORIZED 예외를 발생시킨다.")
        @Test
        fun throwsUnauthorizedWhenAuthenticationFails() {
            // arrange
            val expectedInvalidPw = "WrongPassword1!"
            val request = MockHttpServletRequest().apply {
                addHeader(LOGIN_ID_HEADER, DEFAULT_LOGIN_ID)
                addHeader(LOGIN_PW_HEADER, expectedInvalidPw)
            }
            every {
                userService.authenticate(DEFAULT_LOGIN_ID, expectedInvalidPw)
            } throws CoreException(ErrorType.UNAUTHORIZED, "비밀번호가 일치하지 않습니다.")

            // act & assert
            assertThatThrownBy { sut.preHandle(request, response, handler) }
                .isInstanceOf(CoreException::class.java)
                .extracting("errorType")
                .isEqualTo(ErrorType.UNAUTHORIZED)
        }
    }
}
