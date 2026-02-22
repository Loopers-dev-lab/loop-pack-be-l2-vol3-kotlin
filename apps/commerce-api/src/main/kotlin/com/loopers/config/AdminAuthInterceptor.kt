package com.loopers.config

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AdminAuthInterceptor : HandlerInterceptor {
    companion object {
        private const val ADMIN_HEADER = "X-Loopers-Ldap"
        private const val ADMIN_HEADER_VALUE = "loopers.admin"
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val ldap = request.getHeader(ADMIN_HEADER)
            ?: throw CoreException(ErrorType.UNAUTHORIZED, "인증 헤더 '$ADMIN_HEADER'이(가) 누락되었습니다.")
        if (ldap != ADMIN_HEADER_VALUE) {
            throw CoreException(ErrorType.UNAUTHORIZED, "유효하지 않은 LDAP 인증 정보입니다.")
        }
        return true
    }
}
