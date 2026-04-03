package com.loopers.interfaces.api.interceptor

import com.loopers.application.user.auth.UserAuthenticateUseCase
import com.loopers.application.user.queue.EntryTokenValidationUseCase
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class EntryTokenInterceptor(
    private val userAuthenticateUseCase: UserAuthenticateUseCase,
    private val entryTokenValidationUseCase: EntryTokenValidationUseCase,
) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (request.method != "POST") return true

        val loginId = request.getHeader("X-Loopers-LoginId")
            ?: throw CoreException(ErrorType.ENTRY_TOKEN_REQUIRED)
        val password = request.getHeader("X-Loopers-LoginPw")
            ?: throw CoreException(ErrorType.ENTRY_TOKEN_REQUIRED)
        val userId = userAuthenticateUseCase.authenticateAndGetId(loginId, password)

        val token = request.getHeader("X-Entry-Token")
            ?: throw CoreException(ErrorType.ENTRY_TOKEN_REQUIRED)

        if (!entryTokenValidationUseCase.validate(userId, token)) {
            throw CoreException(ErrorType.ENTRY_TOKEN_INVALID)
        }

        return true
    }
}
