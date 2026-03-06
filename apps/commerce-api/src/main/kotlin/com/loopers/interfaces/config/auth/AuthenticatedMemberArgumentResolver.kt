package com.loopers.interfaces.config.auth

import com.loopers.application.error.ApplicationException
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class AuthenticatedMemberArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.parameterType == AuthenticatedMember::class.java
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): AuthenticatedMember {
        return webRequest.getAttribute(
            MemberAuthenticationInterceptor.AUTHENTICATED_MEMBER_ATTRIBUTE,
            RequestAttributes.SCOPE_REQUEST,
        ) as? AuthenticatedMember
            ?: throw ApplicationException(httpStatus = 401, code = "Unauthorized", message = "인증 정보가 없습니다.")
    }
}
