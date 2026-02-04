package com.loopers.interfaces.api.auth

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.core.MethodParameter
import org.springframework.web.context.request.NativeWebRequest

class AuthenticatedUserArgumentResolverTest {

    private lateinit var resolver: AuthenticatedUserArgumentResolver
    private lateinit var webRequest: NativeWebRequest

    @BeforeEach
    fun setUp() {
        resolver = AuthenticatedUserArgumentResolver()
        webRequest = mockk()
    }

    @Test
    fun `AuthUser가 있으면 반환한다`() {
        val authUser = AuthUser(id = 1L, loginId = "testuser")

        every {
            webRequest.getAttribute(AuthenticationFilter.AUTH_USER_ATTRIBUTE, NativeWebRequest.SCOPE_REQUEST)
        } returns authUser

        val result = resolver.resolveArgument(mockk(), null, webRequest, null)

        assertThat(result.id).isEqualTo(1L)
        assertThat(result.loginId).isEqualTo("testuser")
    }

    @Test
    fun `AuthUser가 없고 인증 시도도 없으면 인증 정보가 필요합니다 예외가 발생한다`() {
        every {
            webRequest.getAttribute(AuthenticationFilter.AUTH_USER_ATTRIBUTE, NativeWebRequest.SCOPE_REQUEST)
        } returns null
        every {
            webRequest.getAttribute(AuthenticationFilter.AUTH_FAILED_ATTRIBUTE, NativeWebRequest.SCOPE_REQUEST)
        } returns null

        val exception = assertThrows<CoreException> {
            resolver.resolveArgument(mockk(), null, webRequest, null)
        }

        assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        assertThat(exception.message).isEqualTo("인증 정보가 필요합니다.")
    }

    @Test
    fun `인증 시도했으나 실패하면 인증에 실패했습니다 예외가 발생한다`() {
        every {
            webRequest.getAttribute(AuthenticationFilter.AUTH_USER_ATTRIBUTE, NativeWebRequest.SCOPE_REQUEST)
        } returns null
        every {
            webRequest.getAttribute(AuthenticationFilter.AUTH_FAILED_ATTRIBUTE, NativeWebRequest.SCOPE_REQUEST)
        } returns true

        val exception = assertThrows<CoreException> {
            resolver.resolveArgument(mockk(), null, webRequest, null)
        }

        assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        assertThat(exception.message).isEqualTo("인증에 실패했습니다.")
    }

    @Test
    fun `AuthenticatedUser 어노테이션과 AuthUser 타입이면 지원한다`() {
        val parameter = mockk<MethodParameter>()
        every { parameter.hasParameterAnnotation(AuthenticatedUser::class.java) } returns true
        every { parameter.parameterType } returns AuthUser::class.java

        assertThat(resolver.supportsParameter(parameter)).isTrue()
    }

    @Test
    fun `어노테이션이 없으면 지원하지 않는다`() {
        val parameter = mockk<MethodParameter>()
        every { parameter.hasParameterAnnotation(AuthenticatedUser::class.java) } returns false
        every { parameter.parameterType } returns AuthUser::class.java

        assertThat(resolver.supportsParameter(parameter)).isFalse()
    }
}
