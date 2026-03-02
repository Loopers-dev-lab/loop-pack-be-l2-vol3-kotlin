package com.loopers.interfaces.api.auth

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class LdapAuthInterceptor : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val ldapHeader = request.getHeader("X-Loopers-Ldap")
        if (ldapHeader != "loopers.admin") {
            throw CoreException(ErrorType.UNAUTHORIZED, "LDAP 인증이 필요합니다.")
        }
        return true
    }
}
