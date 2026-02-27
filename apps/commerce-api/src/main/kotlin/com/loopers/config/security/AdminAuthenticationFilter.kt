package com.loopers.config.security

import com.loopers.support.auth.AdminUser
import com.loopers.support.constant.HttpHeaders
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class AdminAuthenticationFilter : OncePerRequestFilter() {

    companion object {
        private const val VALID_LDAP = "loopers.admin"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val ldap = request.getHeader(HttpHeaders.LDAP)

        if (ldap.isNullOrBlank()) {
            filterChain.doFilter(request, response)
            return
        }

        if (ldap != VALID_LDAP) {
            filterChain.doFilter(request, response)
            return
        }

        val adminUser = AdminUser(ldap = ldap)
        val authentication = UsernamePasswordAuthenticationToken(adminUser, null, emptyList())
        SecurityContextHolder.getContext().authentication = authentication
        filterChain.doFilter(request, response)
    }
}
