package com.loopers.infrastructure.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.domain.auth.JwtTokenProvider
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
        const val AUTHENTICATED_MEMBER_ATTRIBUTE = "authenticatedMember"

        private val AUTHENTICATED_PATHS = listOf(
            "/api/v1/members/me",
        )
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return AUTHENTICATED_PATHS.none { path.startsWith(it) }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authHeader = request.getHeader(AUTHORIZATION_HEADER)

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            writeErrorResponse(response, ErrorType.UNAUTHORIZED)
            return
        }

        val token = authHeader.substring(BEARER_PREFIX.length)

        try {
            val authenticatedMember = jwtTokenProvider.validateToken(token)
            request.setAttribute(AUTHENTICATED_MEMBER_ATTRIBUTE, authenticatedMember)
            filterChain.doFilter(request, response)
        } catch (e: CoreException) {
            writeErrorResponse(response, e.errorType, e.customMessage)
        }
    }

    private fun writeErrorResponse(
        response: HttpServletResponse,
        errorType: ErrorType,
        customMessage: String? = null,
    ) {
        response.status = errorType.status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"

        val apiResponse = ApiResponse.fail(
            errorCode = errorType.code,
            errorMessage = customMessage ?: errorType.message,
        )
        response.writer.write(objectMapper.writeValueAsString(apiResponse))
    }
}
