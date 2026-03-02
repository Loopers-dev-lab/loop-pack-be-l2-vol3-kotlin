package com.loopers.config.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.error.ErrorType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint

class SecurityAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        val errorType = ErrorType.UNAUTHORIZED
        response.status = errorType.status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        val body = ApiResponse.fail(errorType.code, errorType.message)
        objectMapper.writeValue(response.writer, body)
    }
}
