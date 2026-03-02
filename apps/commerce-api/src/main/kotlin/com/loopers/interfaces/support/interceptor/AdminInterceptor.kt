package com.loopers.interfaces.support.interceptor

import com.loopers.interfaces.support.HEADER_LDAP
import com.loopers.interfaces.support.LDAP_ADMIN_VALUE
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AdminInterceptor : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val ldap = request.getHeader(HEADER_LDAP)
        if (ldap == null || ldap != LDAP_ADMIN_VALUE) {
            throw CoreException(ErrorType.UNAUTHORIZED, "어드민 인증이 필요합니다.")
        }
        return true
    }
}
