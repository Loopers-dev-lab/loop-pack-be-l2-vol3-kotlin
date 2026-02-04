package com.loopers.interfaces.api.auth

import com.loopers.domain.user.LoginId
import com.loopers.domain.user.PasswordEncoder
import com.loopers.domain.user.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class AuthenticationFilter(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val loginId = request.getHeader(HEADER_LOGIN_ID)
        val loginPw = request.getHeader(HEADER_LOGIN_PW)

        if (loginId != null && loginPw != null) {
            authenticate(loginId, loginPw)?.let { authUser ->
                request.setAttribute(AUTH_USER_ATTRIBUTE, authUser)
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun authenticate(loginId: String, password: String): AuthUser? {
        val user = runCatching { LoginId(loginId) }
            .getOrNull()
            ?.let { userRepository.findByLoginId(it) }
            ?: return null

        if (!user.authenticate(password, passwordEncoder)) {
            return null
        }

        return AuthUser(
            id = user.persistenceId!!,
            loginId = user.loginId.value,
        )
    }

    companion object {
        const val HEADER_LOGIN_ID = "X-Loopers-LoginId"
        const val HEADER_LOGIN_PW = "X-Loopers-LoginPw"
        const val AUTH_USER_ATTRIBUTE = "authUser"
    }
}
