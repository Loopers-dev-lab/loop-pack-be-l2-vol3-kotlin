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

    /**
     * Extracts credentials from request headers, attempts authentication, and attaches the result to the request before continuing the filter chain.
     *
     * Reads headers "X-Loopers-LoginId" and "X-Loopers-LoginPw". If both are present, attempts to authenticate; on success sets the request attribute "authUser" to an AuthUser, on failure sets the request attribute "authFailed" to `true`. Always delegates to the next filter in the chain.
     *
     * @param request HTTP servlet request; may contain the authentication headers and will receive authentication attributes ("authUser" or "authFailed").
     * @param response HTTP servlet response.
     * @param filterChain The filter chain to continue processing the request.
     */
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val loginId = request.getHeader(HEADER_LOGIN_ID)
        val loginPw = request.getHeader(HEADER_LOGIN_PW)

        if (loginId != null && loginPw != null) {
            val authUser = authenticate(loginId, loginPw)
            if (authUser != null) {
                request.setAttribute(AUTH_USER_ATTRIBUTE, authUser)
            } else {
                request.setAttribute(AUTH_FAILED_ATTRIBUTE, true)
            }
        }

        filterChain.doFilter(request, response)
    }

    /**
     * Authenticates the given credentials and, on success, produces an AuthUser representing the authenticated user.
     *
     * @return An AuthUser for the matched user, or `null` if the loginId is invalid, no user is found, or the password does not match.
     */
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
        const val AUTH_FAILED_ATTRIBUTE = "authFailed"
    }
}