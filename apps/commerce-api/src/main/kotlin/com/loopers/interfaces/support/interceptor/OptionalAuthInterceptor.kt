package com.loopers.interfaces.support.interceptor

import com.loopers.application.user.AuthenticateUserUseCase
import com.loopers.interfaces.support.ATTRIBUTE_USER_ID
import com.loopers.interfaces.support.HEADER_LOGIN_ID
import com.loopers.interfaces.support.HEADER_LOGIN_PW
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class OptionalAuthInterceptor(
    private val authenticateUserUseCase: AuthenticateUserUseCase,
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (request.getAttribute(ATTRIBUTE_USER_ID) != null) return true

        val loginId = request.getHeader(HEADER_LOGIN_ID) ?: return true
        val loginPw = request.getHeader(HEADER_LOGIN_PW) ?: return true

        authenticateUserUseCase.execute(loginId, loginPw)?.let {
            request.setAttribute(ATTRIBUTE_USER_ID, it)
        }

        return true
    }
}
