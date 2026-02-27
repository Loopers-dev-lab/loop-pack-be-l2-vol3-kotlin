package com.loopers.config.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.domain.user.UserService
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.auth.LoginUser
import com.loopers.support.constant.HttpHeaders
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class UserAuthenticationFilter(
    private val userService: UserService,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val loginId = request.getHeader(HttpHeaders.LOGIN_ID)
        val loginPw = request.getHeader(HttpHeaders.LOGIN_PW)

        if (loginId.isNullOrBlank() || loginPw.isNullOrBlank()) {
            filterChain.doFilter(request, response)
            return
        }

        try {
            val user = userService.authenticate(loginId, loginPw)
            val loginUser = LoginUser(id = user.id, loginId = user.loginId)
            val authentication = UsernamePasswordAuthenticationToken(loginUser, null, emptyList())
            SecurityContextHolder.getContext().authentication = authentication
            filterChain.doFilter(request, response)
        } catch (e: CoreException) {
            writeErrorResponse(response, e.errorType)
        }
    }

    private fun writeErrorResponse(response: HttpServletResponse, errorType: ErrorType) {
        response.status = errorType.status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        val body = ApiResponse.fail(errorType.code, errorType.message)
        objectMapper.writeValue(response.writer, body)
    }
}
