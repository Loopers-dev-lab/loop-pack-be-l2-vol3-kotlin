package com.loopers.infrastructure.interceptor

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.PrintWriter
import java.io.StringWriter

class LdapAuthInterceptorTest {
    private val interceptor = LdapAuthInterceptor()
    private val request: HttpServletRequest = mock()
    private val response: HttpServletResponse = mock()
    private val handler: Any = mock()

    @DisplayName("유효한 LDAP 헤더가 모두 있고 올바른 role이면 true를 반환한다")
    @Test
    fun preHandle_shouldReturnTrue_whenValidHeadersProvided() {
        // arrange
        whenever(request.getHeader("X-LDAP-Username")).thenReturn("admin")
        whenever(request.getHeader("X-LDAP-Role")).thenReturn("ADMIN")

        // act
        val result = interceptor.preHandle(request, response, handler)

        // assert
        assertThat(result).isTrue()
    }

    @DisplayName("username 헤더가 없으면 false를 반환하고 401 응답을 보낸다")
    @Test
    fun preHandle_shouldReturnFalse_whenUsernameHeaderIsNull() {
        // arrange
        whenever(request.getHeader("X-LDAP-Username")).thenReturn(null)
        whenever(request.getHeader("X-LDAP-Role")).thenReturn("ADMIN")
        setupResponseWriter()

        // act
        val result = interceptor.preHandle(request, response, handler)

        // assert
        assertThat(result).isFalse()
        verify(response).status = HttpServletResponse.SC_UNAUTHORIZED
    }

    @DisplayName("role 헤더가 없으면 false를 반환하고 401 응답을 보낸다")
    @Test
    fun preHandle_shouldReturnFalse_whenRoleHeaderIsNull() {
        // arrange
        whenever(request.getHeader("X-LDAP-Username")).thenReturn("admin")
        whenever(request.getHeader("X-LDAP-Role")).thenReturn(null)
        setupResponseWriter()

        // act
        val result = interceptor.preHandle(request, response, handler)

        // assert
        assertThat(result).isFalse()
        verify(response).status = HttpServletResponse.SC_UNAUTHORIZED
    }

    @DisplayName("유효하지 않은 role 값이면 false를 반환하고 401 응답을 보낸다")
    @Test
    fun preHandle_shouldReturnFalse_whenRoleIsInvalid() {
        // arrange
        whenever(request.getHeader("X-LDAP-Username")).thenReturn("admin")
        whenever(request.getHeader("X-LDAP-Role")).thenReturn("INVALID_ROLE")
        setupResponseWriter()

        // act
        val result = interceptor.preHandle(request, response, handler)

        // assert
        assertThat(result).isFalse()
        verify(response).status = HttpServletResponse.SC_UNAUTHORIZED
    }

    @DisplayName("username이 빈 문자열이면 false를 반환하고 401 응답을 보낸다")
    @Test
    fun preHandle_shouldReturnFalse_whenUsernameIsBlank() {
        // arrange
        whenever(request.getHeader("X-LDAP-Username")).thenReturn("")
        whenever(request.getHeader("X-LDAP-Role")).thenReturn("ADMIN")
        setupResponseWriter()

        // act
        val result = interceptor.preHandle(request, response, handler)

        // assert
        assertThat(result).isFalse()
        verify(response).status = HttpServletResponse.SC_UNAUTHORIZED
    }

    @DisplayName("role이 빈 문자열이면 false를 반환하고 401 응답을 보낸다")
    @Test
    fun preHandle_shouldReturnFalse_whenRoleIsBlank() {
        // arrange
        whenever(request.getHeader("X-LDAP-Username")).thenReturn("admin")
        whenever(request.getHeader("X-LDAP-Role")).thenReturn("")
        setupResponseWriter()

        // act
        val result = interceptor.preHandle(request, response, handler)

        // assert
        assertThat(result).isFalse()
        verify(response).status = HttpServletResponse.SC_UNAUTHORIZED
    }

    private fun setupResponseWriter() {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        whenever(response.writer).thenReturn(printWriter)
    }
}
