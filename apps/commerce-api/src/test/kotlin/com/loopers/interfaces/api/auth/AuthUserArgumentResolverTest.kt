package com.loopers.interfaces.api.auth

import com.loopers.interfaces.api.ATTRIBUTE_USER_ID
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.core.MethodParameter
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.context.request.ServletWebRequest

class AuthUserArgumentResolverTest {

    private lateinit var resolver: AuthUserArgumentResolver

    @BeforeEach
    fun setUp() {
        resolver = AuthUserArgumentResolver()
    }

    @Nested
    @DisplayName("supportsParameter 호출 시")
    inner class SupportsParameter {

        @Test
        @DisplayName("@AuthUser 어노테이션이 있으면 true를 반환한다")
        fun supportsParameter_withAuthUserAnnotation_returnsTrue() {
            // arrange
            val parameter = getMethodParameter("withAuthUser")

            // act & assert
            assertThat(resolver.supportsParameter(parameter)).isTrue()
        }

        @Test
        @DisplayName("@AuthUser 어노테이션이 없으면 false를 반환한다")
        fun supportsParameter_withoutAuthUserAnnotation_returnsFalse() {
            // arrange
            val parameter = getMethodParameter("withoutAuthUser")

            // act & assert
            assertThat(resolver.supportsParameter(parameter)).isFalse()
        }
    }

    @Nested
    @DisplayName("resolveArgument 호출 시")
    inner class ResolveArgument {

        @Test
        @DisplayName("request에 userId가 있으면 해당 값을 반환한다")
        fun resolveArgument_withUserId_returnsUserId() {
            // arrange
            val request = MockHttpServletRequest()
            request.setAttribute(ATTRIBUTE_USER_ID, 1L)
            val webRequest: NativeWebRequest = ServletWebRequest(request)
            val parameter = getMethodParameter("withAuthUser")

            // act
            val result = resolver.resolveArgument(parameter, null, webRequest, null)

            // assert
            assertThat(result).isEqualTo(1L)
        }

        @Test
        @DisplayName("request에 userId가 없으면 UNAUTHORIZED 예외가 발생한다")
        fun resolveArgument_withoutUserId_throwsUnauthorized() {
            // arrange
            val request = MockHttpServletRequest()
            val webRequest: NativeWebRequest = ServletWebRequest(request)
            val parameter = getMethodParameter("withAuthUser")

            // act
            val exception = assertThrows<CoreException> {
                resolver.resolveArgument(parameter, null, webRequest, null)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    private fun withAuthUser(@AuthUser userId: Long) {}

    @Suppress("unused", "UNUSED_PARAMETER")
    private fun withoutAuthUser(userId: Long) {}

    private fun getMethodParameter(methodName: String): MethodParameter {
        val method = this::class.java.getDeclaredMethod(methodName, Long::class.java)
        return MethodParameter(method, 0)
    }
}
