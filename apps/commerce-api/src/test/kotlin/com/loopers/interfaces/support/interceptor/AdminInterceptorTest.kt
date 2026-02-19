package com.loopers.interfaces.support.interceptor

import com.loopers.interfaces.support.HEADER_LDAP
import com.loopers.interfaces.support.LDAP_ADMIN_VALUE
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

class AdminInterceptorTest {

    private lateinit var adminInterceptor: AdminInterceptor

    @BeforeEach
    fun setUp() {
        adminInterceptor = AdminInterceptor()
    }

    @Nested
    @DisplayName("preHandle 호출 시")
    inner class PreHandle {

        @Test
        @DisplayName("유효한 LDAP 헤더가 있으면 true를 반환한다")
        fun preHandle_withValidLdapHeader_returnsTrue() {
            // arrange
            val request = MockHttpServletRequest()
            request.addHeader(HEADER_LDAP, LDAP_ADMIN_VALUE)

            // act
            val result = adminInterceptor.preHandle(request, MockHttpServletResponse(), Any())

            // assert
            assertThat(result).isTrue()
        }

        @Test
        @DisplayName("LDAP 헤더가 없으면 UNAUTHORIZED 예외가 발생한다")
        fun preHandle_withoutLdapHeader_throwsUnauthorized() {
            // arrange
            val request = MockHttpServletRequest()

            // act
            val exception = assertThrows<CoreException> {
                adminInterceptor.preHandle(request, MockHttpServletResponse(), Any())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }

        @Test
        @DisplayName("LDAP 헤더 값이 일치하지 않으면 UNAUTHORIZED 예외가 발생한다")
        fun preHandle_withInvalidLdapValue_throwsUnauthorized() {
            // arrange
            val request = MockHttpServletRequest()
            request.addHeader(HEADER_LDAP, "invalid.value")

            // act
            val exception = assertThrows<CoreException> {
                adminInterceptor.preHandle(request, MockHttpServletResponse(), Any())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }
    }
}
