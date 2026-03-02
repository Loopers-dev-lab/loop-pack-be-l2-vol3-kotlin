package com.loopers.interfaces.support.interceptor

import com.loopers.application.user.AuthenticateUserUseCase
import com.loopers.domain.user.UserTestFixture
import com.loopers.interfaces.support.ATTRIBUTE_USER_ID
import com.loopers.interfaces.support.HEADER_LOGIN_ID
import com.loopers.interfaces.support.HEADER_LOGIN_PW
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

    private lateinit var authenticateUserUseCase: AuthenticateUserUseCase
    private lateinit var authInterceptor: AuthInterceptor

    @BeforeEach
    fun setUp() {
        authenticateUserUseCase = mockk()
        authInterceptor = AuthInterceptor(authenticateUserUseCase)
    }

    @Nested
    @DisplayName("preHandle 호출 시")
    inner class PreHandle {

        @Test
        @DisplayName("인증 성공 시 request에 userId를 설정하고 true를 반환한다")
        fun preHandle_withValidCredentials_setsUserIdAndReturnsTrue() {
            // arrange
            val request = MockHttpServletRequest()
            request.addHeader(HEADER_LOGIN_ID, UserTestFixture.DEFAULT_LOGIN_ID)
            request.addHeader(HEADER_LOGIN_PW, UserTestFixture.DEFAULT_PASSWORD)

            val user = UserTestFixture.createUser()
            every { authenticateUserUseCase.execute(UserTestFixture.DEFAULT_LOGIN_ID, UserTestFixture.DEFAULT_PASSWORD) } returns user.id.value

            // act
            val result = authInterceptor.preHandle(request, MockHttpServletResponse(), Any())

            // assert
            assertThat(result).isTrue()
            assertThat(request.getAttribute(ATTRIBUTE_USER_ID)).isEqualTo(user.id.value)
        }

        @Test
        @DisplayName("로그인 ID 헤더가 없으면 UNAUTHORIZED 예외가 발생한다")
        fun preHandle_withoutLoginIdHeader_throwsUnauthorized() {
            // arrange
            val request = MockHttpServletRequest()
            request.addHeader(HEADER_LOGIN_PW, "Password1!")

            // act
            val exception = assertThrows<CoreException> {
                authInterceptor.preHandle(request, MockHttpServletResponse(), Any())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }

        @Test
        @DisplayName("비밀번호 헤더가 없으면 UNAUTHORIZED 예외가 발생한다")
        fun preHandle_withoutLoginPwHeader_throwsUnauthorized() {
            // arrange
            val request = MockHttpServletRequest()
            request.addHeader(HEADER_LOGIN_ID, "testuser")

            // act
            val exception = assertThrows<CoreException> {
                authInterceptor.preHandle(request, MockHttpServletResponse(), Any())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 인증하면 UNAUTHORIZED 예외가 발생한다")
        fun preHandle_userNotFound_throwsUnauthorized() {
            // arrange
            val request = MockHttpServletRequest()
            request.addHeader(HEADER_LOGIN_ID, "nonexistent")
            request.addHeader(HEADER_LOGIN_PW, "Password1!")

            every { authenticateUserUseCase.execute("nonexistent", "Password1!") } returns null

            // act
            val exception = assertThrows<CoreException> {
                authInterceptor.preHandle(request, MockHttpServletResponse(), Any())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
            assertThat(exception.message).isEqualTo("인증에 실패했습니다.")
        }

        @Test
        @DisplayName("잘못된 비밀번호로 인증하면 UNAUTHORIZED 예외가 발생한다")
        fun preHandle_wrongPassword_throwsUnauthorized() {
            // arrange
            val request = MockHttpServletRequest()
            request.addHeader(HEADER_LOGIN_ID, UserTestFixture.DEFAULT_LOGIN_ID)
            request.addHeader(HEADER_LOGIN_PW, "WrongPass1!")

            every { authenticateUserUseCase.execute(UserTestFixture.DEFAULT_LOGIN_ID, "WrongPass1!") } returns null

            // act
            val exception = assertThrows<CoreException> {
                authInterceptor.preHandle(request, MockHttpServletResponse(), Any())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
            assertThat(exception.message).isEqualTo("인증에 실패했습니다.")
        }
    }
}
