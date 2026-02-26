package com.loopers.support.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.interfaces.common.ApiResponse
import com.loopers.support.error.ErrorType
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.stereotype.Component

@Component
@Order(2)
class AdminAuthenticationFilter(
    private val objectMapper: ObjectMapper,
) : Filter {

    companion object {
        private const val LDAP_HEADER = "X-Loopers-Ldap"
        private const val LDAP_EXPECTED_VALUE = "loopers.admin"
        private const val ADMIN_PATH_PREFIX = "/api-admin/"
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        if (requiresAdminAuthentication(httpRequest)) {
            val ldapHeader = httpRequest.getHeader(LDAP_HEADER)

            if (ldapHeader != LDAP_EXPECTED_VALUE) {
                writeErrorResponse(httpResponse)
                return
            }
        }

        chain.doFilter(request, response)
    }

    private fun requiresAdminAuthentication(request: HttpServletRequest): Boolean {
        return request.requestURI.startsWith(ADMIN_PATH_PREFIX)
    }

    private fun writeErrorResponse(response: HttpServletResponse) {
        response.status = ErrorType.UNAUTHORIZED.status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"

        val errorResponse = ApiResponse.fail(ErrorType.UNAUTHORIZED.code, "어드민 인증 헤더가 필요합니다.")
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
}
