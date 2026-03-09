package com.loopers.config

import com.loopers.domain.user.UserService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class UserAuthInterceptor(
    private val userService: UserService,
) : HandlerInterceptor {
    companion object {
        private const val LOGIN_ID_HEADER = "X-Loopers-LoginId"
        private const val LOGIN_PW_HEADER = "X-Loopers-LoginPw"
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (request.requestURI == "/api/v1/users" && request.method == "POST") {
            return true  // 회원가입만 통과
        }
        val loginId = request.getHeader(LOGIN_ID_HEADER)
            ?: throw CoreException(ErrorType.UNAUTHORIZED, "인증 헤더 '$LOGIN_ID_HEADER'이(가) 누락되었습니다.")
        val loginPw = request.getHeader(LOGIN_PW_HEADER)
            ?: throw CoreException(ErrorType.UNAUTHORIZED, "인증 헤더 '$LOGIN_PW_HEADER'이(가) 누락되었습니다.")
        userService.authenticate(loginId, loginPw)
        return true
    }
}
