package com.loopers.infrastructure.filter

import com.loopers.domain.user.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class AuthenticationFilter(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) : OncePerRequestFilter() {

    companion object {
        private const val HEADER_LOGIN_ID = "X-Loopers-LoginId"
        private const val HEADER_LOGIN_PW = "X-Loopers-LoginPw"
    }
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val loginId = request.getHeader(HEADER_LOGIN_ID)
        val loginPw = request.getHeader(HEADER_LOGIN_PW)

        if (loginId.isNullOrBlank() || loginPw.isNullOrBlank()) {
            filterChain.doFilter(request, response)
            return
        }

        val user = userRepository.findByLoginId(loginId)
        if (user == null || !passwordEncoder.matches(loginPw, user.password)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "로그인 아이디 또는 패스워드가 잘못되었습니다.")
            return
        }

        val authenticationToken = UsernamePasswordAuthenticationToken(
            user.id,
            null,
            emptyList(),
        )

        SecurityContextHolder.getContext().authentication = authenticationToken

        filterChain.doFilter(request, response)
    }
}
