package com.loopers.interfaces.config.auth

import com.loopers.application.auth.AuthResult
import com.loopers.application.auth.AuthService
import com.loopers.application.error.ApplicationException
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
import org.springframework.web.method.HandlerMethod

class MemberAuthenticationInterceptorTest {

    private lateinit var interceptor: MemberAuthenticationInterceptor
    private lateinit var authService: AuthService

    private val testAuthResult = AuthResult(id = 1L, loginId = "testuser01")

    @BeforeEach
    fun setUp() {
        authService = mockk()
        interceptor = MemberAuthenticationInterceptor(authService)
    }

    private fun createAuthenticatedHandlerMethod(): HandlerMethod {
        val method = MethodAnnotatedTestController::class.java.getMethod("authenticatedEndpoint")
        return HandlerMethod(MethodAnnotatedTestController(), method)
    }

    private fun createClassAnnotatedHandlerMethod(): HandlerMethod {
        val method = ClassAnnotatedTestController::class.java.getMethod("authenticatedEndpoint")
        return HandlerMethod(ClassAnnotatedTestController(), method)
    }

    private fun createRequest(loginId: String?, password: String?): MockHttpServletRequest {
        return MockHttpServletRequest().apply {
            loginId?.let { addHeader(MemberAuthenticationInterceptor.HEADER_LOGIN_ID, it) }
            password?.let { addHeader(MemberAuthenticationInterceptor.HEADER_LOGIN_PW, it) }
        }
    }

    @DisplayName("회원 인증 인터셉터 동작 검증")
    @Nested
    inner class MemberAuthentication {

        @DisplayName("유효한 인증 정보가 주어지면, AuthenticatedMember를 request attribute에 설정한다.")
        @Test
        fun setsAuthenticatedMemberAttribute_whenCredentialsAreValid() {
            // arrange
            every { authService.authenticate("testuser01", "TestPass123!") } returns testAuthResult
            val request = createRequest("testuser01", "TestPass123!")
            val response = MockHttpServletResponse()
            val handler = createAuthenticatedHandlerMethod()

            // act
            val result = interceptor.preHandle(request, response, handler)

            // assert
            assertThat(result).isTrue()
            val authenticatedMember = request.getAttribute(
                MemberAuthenticationInterceptor.AUTHENTICATED_MEMBER_ATTRIBUTE,
            ) as AuthenticatedMember
            assertThat(authenticatedMember.id).isEqualTo(1L)
            assertThat(authenticatedMember.loginId).isEqualTo("testuser01")
        }

        @DisplayName("인증 헤더가 없으면, ApplicationException을 던진다.")
        @Test
        fun throwsApplicationException_whenHeadersAreMissing() {
            // arrange
            val request = createRequest(null, null)
            val response = MockHttpServletResponse()
            val handler = createAuthenticatedHandlerMethod()

            // act & assert
            assertThrows<ApplicationException> {
                interceptor.preHandle(request, response, handler)
            }
        }

        @DisplayName("AuthService에서 예외가 발생하면, 그대로 전파한다.")
        @Test
        fun propagatesException_whenAuthServiceThrows() {
            // arrange
            every { authService.authenticate("testuser01", "WrongPass!") } throws
                ApplicationException(httpStatus = 401, code = "Unauthorized", message = "인증에 실패했습니다.")
            val request = createRequest("testuser01", "WrongPass!")
            val response = MockHttpServletResponse()
            val handler = createAuthenticatedHandlerMethod()

            // act & assert
            assertThrows<ApplicationException> {
                interceptor.preHandle(request, response, handler)
            }
        }

        @DisplayName("클래스 레벨 @MemberAuthenticated 어노테이션이 적용되면 메서드에서도 인증을 수행한다.")
        @Test
        fun performsAuthentication_whenClassLevelAnnotationIsPresent() {
            // arrange
            every { authService.authenticate("testuser01", "TestPass123!") } returns testAuthResult
            val request = createRequest("testuser01", "TestPass123!")
            val response = MockHttpServletResponse()
            val handler = createClassAnnotatedHandlerMethod()

            // act
            val result = interceptor.preHandle(request, response, handler)

            // assert
            assertThat(result).isTrue()
            val authenticatedMember = request.getAttribute(
                MemberAuthenticationInterceptor.AUTHENTICATED_MEMBER_ATTRIBUTE,
            ) as AuthenticatedMember
            assertThat(authenticatedMember.id).isEqualTo(1L)
            assertThat(authenticatedMember.loginId).isEqualTo("testuser01")
        }
    }

    // 메서드 레벨 @MemberAuthenticated 테스트용 컨트롤러
    class MethodAnnotatedTestController {
        @MemberAuthenticated
        fun authenticatedEndpoint() {}
    }

    // 클래스 레벨 @MemberAuthenticated 테스트용 컨트롤러
    @MemberAuthenticated
    class ClassAnnotatedTestController {
        fun authenticatedEndpoint() {}
    }
}
