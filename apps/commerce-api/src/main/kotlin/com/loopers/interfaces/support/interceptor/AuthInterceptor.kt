package com.loopers.interfaces.support.interceptor

import com.loopers.domain.user.UserService
import com.loopers.interfaces.support.ATTRIBUTE_USER_ID
import com.loopers.interfaces.support.HEADER_LOGIN_ID
import com.loopers.interfaces.support.HEADER_LOGIN_PW
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AuthInterceptor(
    private val userService: UserService,
) : HandlerInterceptor {

    // TODO: 추후 security 추가 시 변경
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val loginId = request.getHeader(HEADER_LOGIN_ID)
            ?: throw CoreException(ErrorType.UNAUTHORIZED, "인증 헤더가 누락되었습니다.")

        val loginPw = request.getHeader(HEADER_LOGIN_PW)
            ?: throw CoreException(ErrorType.UNAUTHORIZED, "인증 헤더가 누락되었습니다.")

        val user = userService.getUserInfo(loginId)
            ?: throw CoreException(ErrorType.UNAUTHORIZED, "인증에 실패했습니다.")

        if (!user.verifyPassword(loginPw)) {
            throw CoreException(ErrorType.UNAUTHORIZED, "인증에 실패했습니다.")
        }

        request.setAttribute(ATTRIBUTE_USER_ID, user.id)

        return true
    }
}
