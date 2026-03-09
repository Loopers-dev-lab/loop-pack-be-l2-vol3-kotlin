package com.loopers.config

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

@DisplayName("AdminAuthInterceptor")
class AdminAuthInterceptorTest {
    companion object {
        private const val ADMIN_HEADER = "X-Loopers-Ldap"
        private const val VALID_LDAP_VALUE = "loopers.admin"
        private const val INVALID_LDAP_VALUE = "invalid.value"
    }

    private val sut = AdminAuthInterceptor()
    private val response = MockHttpServletResponse()
    private val handler = Any()

    @DisplayName("preHandle")
    @Nested
    inner class PreHandle {
        @DisplayName("유효한 LDAP 값이면, true를 반환한다.")
        @Test
        fun returnsTrueWhenValidLdapValueIsProvided() {
            // arrange
            val request = MockHttpServletRequest().apply {
                addHeader(ADMIN_HEADER, VALID_LDAP_VALUE)
            }

            // act
            val result = sut.preHandle(request, response, handler)

            // assert
            assertThat(result).isTrue()
        }

        @DisplayName("잘못된 LDAP 값이면, UNAUTHORIZED 예외가 발생한다.")
        @Test
        fun throwsUnauthorizedWhenInvalidLdapValueIsProvided() {
            // arrange
            val request = MockHttpServletRequest().apply {
                addHeader(ADMIN_HEADER, INVALID_LDAP_VALUE)
            }

            // act & assert
            assertThatThrownBy { sut.preHandle(request, response, handler) }
                .isInstanceOf(CoreException::class.java)
                .extracting("errorType")
                .isEqualTo(ErrorType.UNAUTHORIZED)
        }

        @DisplayName("헤더가 없으면, UNAUTHORIZED 예외가 발생한다.")
        @Test
        fun throwsUnauthorizedWhenHeaderIsMissing() {
            // arrange
            val request = MockHttpServletRequest()

            // act & assert
            assertThatThrownBy { sut.preHandle(request, response, handler) }
                .isInstanceOf(CoreException::class.java)
                .extracting("errorType")
                .isEqualTo(ErrorType.UNAUTHORIZED)
        }
    }
}
