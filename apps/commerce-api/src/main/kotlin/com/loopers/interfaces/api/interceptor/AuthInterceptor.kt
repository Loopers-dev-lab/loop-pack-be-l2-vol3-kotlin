package com.loopers.interfaces.api.interceptor

import com.loopers.application.auth.AuthService
import com.loopers.interfaces.api.HEADER_LOGIN_ID
import com.loopers.interfaces.api.HEADER_LOGIN_PW
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AuthInterceptor(
    private val authService: AuthService
) : HandlerInterceptor {

    //TODO: 추후 security 추가 시 변경
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val loginId = request.getHeader(HEADER_LOGIN_ID)
            ?: throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID 헤더가 필요합니다.")

        val loginPw = request.getHeader(HEADER_LOGIN_PW)
            ?: throw CoreException(ErrorType.BAD_REQUEST, "비밀번호 헤더가 필요합니다.")

        authService.authenticate(loginId, loginPw)

        return true
    }
}
