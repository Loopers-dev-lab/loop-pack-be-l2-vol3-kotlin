package com.loopers.infrastructure.interceptor

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.domain.user.UserRepository
import com.loopers.domain.user.vo.LoginId
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.error.ErrorType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.servlet.HandlerInterceptor

class UserAuthInterceptor(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) : HandlerInterceptor {

    companion object {
        const val USER_ID_ATTRIBUTE = "userId"
        private const val HEADER_LOGIN_ID = "X-Loopers-LoginId"
        private const val HEADER_LOGIN_PW = "X-Loopers-LoginPw"
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        // POST /api/*/users (회원가입)은 인증 불필요
        if (request.method == "POST" && request.requestURI.matches(Regex(".*/api/[^/]+/users$"))) {
            return true
        }

        val loginId = request.getHeader(HEADER_LOGIN_ID)
        val loginPw = request.getHeader(HEADER_LOGIN_PW)

        if (loginId.isNullOrBlank() || loginPw.isNullOrBlank()) {
            return sendUnauthorizedResponse(response, "인증이 필요합니다.")
        }

        val user = userRepository.findByLoginId(LoginId.of(loginId))
        if (user == null || !passwordEncoder.matches(loginPw, user.password.value)) {
            return sendUnauthorizedResponse(response, "로그인 아이디 또는 패스워드가 잘못되었습니다.")
        }

        request.setAttribute(USER_ID_ATTRIBUTE, user.id)
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
