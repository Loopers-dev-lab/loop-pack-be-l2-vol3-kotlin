package com.loopers.interfaces.api.auth

import com.loopers.domain.user.User
import com.loopers.domain.user.UserAuthService
import com.loopers.support.error.UserException
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class CurrentUserArgumentResolver(
    private val userAuthService: UserAuthService,
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(CurrentUser::class.java) &&
            parameter.parameterType == User::class.java
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): User {
        val loginId = webRequest.getHeader(HEADER_LOGIN_ID)
            ?: throw UserException.invalidCredentials()
        val password = webRequest.getHeader(HEADER_LOGIN_PW)
            ?: throw UserException.invalidCredentials()

        return userAuthService.authenticate(loginId, password)
    }

    companion object {
        private const val HEADER_LOGIN_ID = "X-Loopers-LoginId"
        private const val HEADER_LOGIN_PW = "X-Loopers-LoginPw"
    }
}
