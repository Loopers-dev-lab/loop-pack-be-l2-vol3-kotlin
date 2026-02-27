package com.loopers.infrastructure.interceptor

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.domain.admin.LdapRole
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.error.ErrorType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.servlet.HandlerInterceptor

class LdapAuthInterceptor : HandlerInterceptor {

    companion object {
        private const val HEADER_LDAP_USERNAME = "X-LDAP-Username"
        private const val HEADER_LDAP_ROLE = "X-LDAP-Role"
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val username = request.getHeader(HEADER_LDAP_USERNAME)
        val roleHeader = request.getHeader(HEADER_LDAP_ROLE)

        if (username.isNullOrBlank() || roleHeader.isNullOrBlank()) {
            return sendUnauthorizedResponse(response, "인증이 필요합니다.")
        }

        val ldapRole = runCatching { LdapRole.valueOf(roleHeader) }.getOrNull()
        if (ldapRole == null) {
            return sendUnauthorizedResponse(response, "유효하지 않은 역할입니다.")
        }

        return true
    }

    private fun sendUnauthorizedResponse(response: HttpServletResponse, message: String): Boolean {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json;charset=UTF-8"
        val errorResponse = ApiResponse.fail(
            errorCode = ErrorType.UNAUTHORIZED.code,
            errorMessage = message,
        )
        response.writer.write(ObjectMapper().writeValueAsString(errorResponse))
        return false
    }
}
