package com.loopers.interfaces.api.auth

import com.loopers.domain.user.LoginId
import com.loopers.domain.user.PasswordEncryptor
import com.loopers.domain.user.UserService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class AuthenticationResolver(
    private val userService: UserService,
    private val passwordEncryptor: PasswordEncryptor,
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(RequireAuth::class.java)
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): AuthenticatedUser {
        // 1. Extract headers
        val loginIdValue = webRequest.getHeader("X-Loopers-LoginId")
            ?: throw CoreException(ErrorType.UNAUTHORIZED, "X-Loopers-LoginId 헤더가 필요합니다.")

        val password = webRequest.getHeader("X-Loopers-LoginPw")
            ?: throw CoreException(ErrorType.UNAUTHORIZED, "X-Loopers-LoginPw 헤더가 필요합니다.")

        // 2. Validate LoginId format and convert to value object
        val loginId = try {
            LoginId(loginIdValue)
        } catch (e: IllegalArgumentException) {
            throw CoreException(ErrorType.UNAUTHORIZED, "올바르지 않은 LoginId 형식입니다.")
        }

        // 3. Get user from database
        val user = try {
            userService.getUserByLoginId(loginId)
        } catch (e: CoreException) {
            throw CoreException(ErrorType.UNAUTHORIZED, "인증에 실패했습니다.")
        }

        // 4. Verify password
        if (!passwordEncryptor.matches(password, user.password)) {
            throw CoreException(ErrorType.UNAUTHORIZED, "인증에 실패했습니다.")
        }

        // 5. Return authenticated user context
        return AuthenticatedUser(
            loginId = user.loginId,
            birthDate = user.birthDate,
        )
    }
}
