package com.loopers.interfaces.api.auth

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class AuthenticatedUserArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(AuthenticatedUser::class.java) &&
            parameter.parameterType == AuthUser::class.java
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): AuthUser {
        val authUser = webRequest.getAttribute(
            AuthenticationFilter.AUTH_USER_ATTRIBUTE,
            NativeWebRequest.SCOPE_REQUEST,
        ) as? AuthUser

        if (authUser != null) {
            return authUser
        }

        val authFailed = webRequest.getAttribute(
            AuthenticationFilter.AUTH_FAILED_ATTRIBUTE,
            NativeWebRequest.SCOPE_REQUEST,
        ) as? Boolean ?: false

        if (authFailed) {
            throw CoreException(ErrorType.UNAUTHORIZED, "인증에 실패했습니다.")
        }

        throw CoreException(ErrorType.UNAUTHORIZED, "인증 정보가 필요합니다.")
    }
}
