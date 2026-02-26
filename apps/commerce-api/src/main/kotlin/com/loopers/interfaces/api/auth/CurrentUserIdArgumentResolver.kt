package com.loopers.interfaces.api.auth

import com.loopers.domain.user.UserAuthService
import com.loopers.support.constant.AuthHeaders
import com.loopers.support.error.CoreException
import com.loopers.support.error.UserErrorCode
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class CurrentUserIdArgumentResolver(
    private val userAuthService: UserAuthService,
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(CurrentUserId::class.java) &&
            parameter.parameterType == Long::class.java
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Long {
        val loginId = webRequest.getHeader(AuthHeaders.User.LOGIN_ID)
            ?: throw CoreException(UserErrorCode.AUTHENTICATION_FAILED)
        val password = webRequest.getHeader(AuthHeaders.User.LOGIN_PW)
            ?: throw CoreException(UserErrorCode.AUTHENTICATION_FAILED)

        return userAuthService.authenticateAndGetId(loginId, password)
    }
}
