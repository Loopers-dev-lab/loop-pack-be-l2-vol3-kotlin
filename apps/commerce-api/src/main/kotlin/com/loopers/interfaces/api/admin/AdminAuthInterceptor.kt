package com.loopers.interfaces.api.admin

import com.loopers.domain.admin.AdminService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AdminAuthInterceptor(
    private val adminService: AdminService,
) : HandlerInterceptor {
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val ldap = request.getHeader("X-Loopers-Ldap")
            ?: throw CoreException(ErrorType.UNAUTHORIZED)

        adminService.getAdminByLdap(ldap)
            ?: throw CoreException(ErrorType.UNAUTHORIZED)

        return true
    }
}
